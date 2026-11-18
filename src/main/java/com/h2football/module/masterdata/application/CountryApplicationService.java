package com.h2football.module.masterdata.application;

import com.h2football.module.masterdata.domain.model.Country;
import com.h2football.module.masterdata.domain.repository.CountryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class CountryApplicationService {
    private final CountryRepository countries;
    public CountryApplicationService(CountryRepository countries) { this.countries = countries; }

    public Country create(String isoCode, String name) {
        String normalizedCode = isoCode.trim().toUpperCase();
        if (countries.existsByIsoCode(normalizedCode))
            throw new DuplicateMasterDataException("Country ISO code already exists: " + normalizedCode);
        return countries.save(new Country(normalizedCode, name.trim()));
    }
    @Transactional(readOnly = true)
    public List<Country> findAll() { return countries.findAll(); }
    @Transactional(readOnly = true)
    public Country get(Long id) { return countries.findById(id).orElseThrow(() -> new MasterDataNotFoundException("Country", id)); }
}