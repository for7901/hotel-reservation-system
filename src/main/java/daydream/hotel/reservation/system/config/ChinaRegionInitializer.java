package daydream.hotel.reservation.system.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import daydream.hotel.reservation.system.common.cache.CacheKeys;
import daydream.hotel.reservation.system.common.cache.CacheService;
import daydream.hotel.reservation.system.hotel.entity.City;
import daydream.hotel.reservation.system.hotel.entity.Province;
import daydream.hotel.reservation.system.hotel.mapper.CityMapper;
import daydream.hotel.reservation.system.hotel.mapper.ProvinceMapper;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@Order(1)
public class ChinaRegionInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ChinaRegionInitializer.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Set<String> SKIP_CITY_NAMES =
            Set.of("市辖区", "县", "省直辖县级行政区划", "自治区直辖县级行政区划");
    private static final Set<String> MUNICIPALITY_CODES = Set.of("11", "12", "31", "50");

    private final ProvinceMapper provinceMapper;
    private final CityMapper cityMapper;
    private final CacheService cacheService;

    public ChinaRegionInitializer(
            ProvinceMapper provinceMapper, CityMapper cityMapper, CacheService cacheService) {
        this.provinceMapper = provinceMapper;
        this.cityMapper = cityMapper;
        this.cacheService = cacheService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (provinceMapper.selectCount(null) == 0) {
                seedFromJson();
            } else {
                syncCityProvinceFromJson();
            }
            int backfilled = ensureDefaultCities();
            if (backfilled > 0) {
                log.info("Backfilled {} default cities for provinces without cities", backfilled);
                evictRegionCache();
            }
        } catch (Exception ex) {
            log.error("Failed to initialize China regions", ex);
        }
    }

    private void syncCityProvinceFromJson() throws Exception {
        List<RawProvince> rawProvinces = readJson("data/china/provinces.json", new TypeReference<>() {});
        List<RawCity> rawCities = readJson("data/china/cities.json", new TypeReference<>() {});
        Map<String, Long> provinceCodeToId =
                provinceMapper.selectList(null).stream()
                        .collect(java.util.stream.Collectors.toMap(Province::getCode, Province::getId));

        Map<String, City> legacyByName = new HashMap<>();
        cityMapper.selectList(null).forEach(city -> legacyByName.put(city.getName(), city));

        int inserted = 0;
        int updated = 0;
        for (RawCity raw : rawCities) {
            if (SKIP_CITY_NAMES.contains(raw.name())) {
                continue;
            }
            Long provinceId = provinceCodeToId.get(raw.provinceCode());
            if (provinceId == null) {
                continue;
            }
            String cityName = resolveCityName(raw.name(), raw.provinceCode(), rawProvinces);
            UpsertResult result = upsertCity(legacyByName, provinceId, cityName, raw.code(), raw.name());
            if (result == UpsertResult.INSERTED) {
                inserted++;
            } else if (result == UpsertResult.UPDATED) {
                updated++;
            }
        }
        if (inserted > 0 || updated > 0) {
            log.info("Synced city-province mapping: {} inserted, {} updated", inserted, updated);
            evictRegionCache();
        }
    }

    private void seedFromJson() throws Exception {
        List<RawProvince> rawProvinces = readJson("data/china/provinces.json", new TypeReference<>() {});
        List<RawCity> rawCities = readJson("data/china/cities.json", new TypeReference<>() {});

        Map<String, Long> provinceCodeToId = new HashMap<>();
        for (RawProvince raw : rawProvinces) {
            Province province = new Province();
            province.setName(shortProvinceName(raw.name()));
            province.setCode(raw.code());
            provinceMapper.insert(province);
            provinceCodeToId.put(raw.code(), province.getId());
        }

            Map<String, City> existingByName = new HashMap<>();
            cityMapper.selectList(null).forEach(city -> existingByName.put(city.getName(), city));

            int inserted = 0;
            int updated = 0;
            for (RawCity raw : rawCities) {
                if (SKIP_CITY_NAMES.contains(raw.name())) {
                    continue;
                }
                Long provinceId = provinceCodeToId.get(raw.provinceCode());
                if (provinceId == null) {
                    continue;
                }
                String cityName = resolveCityName(raw.name(), raw.provinceCode(), rawProvinces);
                UpsertResult result =
                        upsertCity(existingByName, provinceId, cityName, raw.code(), raw.name());
                if (result == UpsertResult.INSERTED) {
                    inserted++;
                } else if (result == UpsertResult.UPDATED) {
                    updated++;
                }
            }

        int backfilled = ensureDefaultCities();
        log.info(
                "Initialized China regions: {} provinces, {} cities inserted, {} updated, {} backfilled",
                rawProvinces.size(),
                inserted,
                updated,
                backfilled);
        evictRegionCache();
    }

    private enum UpsertResult {
        INSERTED,
        UPDATED,
        SKIPPED
    }

    private UpsertResult upsertCity(
            Map<String, City> legacyByName, Long provinceId, String cityName, String code, String rawName) {
        City existing =
                cityMapper.selectOne(
                        new LambdaQueryWrapper<City>()
                                .eq(City::getProvinceId, provinceId)
                                .eq(City::getName, cityName)
                                .last("LIMIT 1"));
        if (existing == null) {
            existing = findLegacyCity(legacyByName, cityName, rawName);
            if (existing != null
                    && existing.getProvinceId() != null
                    && !existing.getProvinceId().equals(provinceId)) {
                existing = null;
            }
        }
        if (existing != null) {
            existing.setProvinceId(provinceId);
            existing.setName(cityName);
            if (existing.getCode() == null || existing.getCode().isBlank()) {
                existing.setCode(code);
            }
            cityMapper.updateById(existing);
            legacyByName.put(cityName, existing);
            return UpsertResult.UPDATED;
        }
        City city = new City();
        city.setProvinceId(provinceId);
        city.setName(cityName);
        city.setCode(code);
        cityMapper.insert(city);
        legacyByName.put(cityName, city);
        return UpsertResult.INSERTED;
    }

    private int ensureDefaultCities() {
        int count = 0;
        List<Province> provinces =
                provinceMapper.selectList(new LambdaQueryWrapper<Province>().orderByAsc(Province::getCode));
        for (Province province : provinces) {
            long cityCount =
                    cityMapper.selectCount(
                            new LambdaQueryWrapper<City>().eq(City::getProvinceId, province.getId()));
            if (cityCount > 0) {
                continue;
            }
            City city = new City();
            city.setProvinceId(province.getId());
            city.setName(province.getName());
            city.setCode(province.getCode() + "00");
            cityMapper.insert(city);
            count++;
        }
        return count;
    }

    private void evictRegionCache() {
        cacheService.evict(CacheKeys.PROVINCE_LIST);
        cacheService.evictByPrefix(CacheKeys.CITY_LIST);
    }

    private City findLegacyCity(Map<String, City> existingByName, String cityName, String rawName) {
        City direct = existingByName.get(rawName);
        if (direct != null) {
            return direct;
        }
        if (rawName.endsWith("市") && existingByName.containsKey(rawName.substring(0, rawName.length() - 1))) {
            return existingByName.get(rawName.substring(0, rawName.length() - 1));
        }
        return null;
    }

    private String resolveCityName(String rawName, String provinceCode, List<RawProvince> provinces) {
        if (MUNICIPALITY_CODES.contains(provinceCode)) {
            return provinces.stream()
                    .filter(p -> p.code().equals(provinceCode))
                    .findFirst()
                    .map(p -> shortProvinceName(p.name()))
                    .orElse(stripCitySuffix(rawName));
        }
        return stripCitySuffix(rawName);
    }

    private String shortProvinceName(String name) {
        return name
                .replace("壮族自治区", "")
                .replace("回族自治区", "")
                .replace("维吾尔自治区", "")
                .replace("自治区", "")
                .replace("特别行政区", "")
                .replace("省", "")
                .replace("市", "");
    }

    private String stripCitySuffix(String name) {
        if (name.endsWith("市")) {
            return name.substring(0, name.length() - 1);
        }
        return name;
    }

    private <T> T readJson(String path, TypeReference<T> type) throws Exception {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return OBJECT_MAPPER.readValue(in, type);
        }
    }

    private record RawProvince(String code, String name) {}

    private record RawCity(String code, String name, String provinceCode) {}
}
