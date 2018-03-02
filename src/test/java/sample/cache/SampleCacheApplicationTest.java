/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.cache;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SampleCacheApplicationTest {

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private CountryRepository countryRepository;

	@Autowired
	private LoadingCountryRepository loadingCountryRepository;

	@Test
	public void retrieveCacheViaCacheManager() {
		Cache countries = this.cacheManager.getCache("countriesViaCacheManager");
		assertThat(countries).isNotNull();
		countries.clear(); // Simple test assuming the cache is empty
		assertThat(countries.get("BE")).isNull();
	}

	/**
	 * Check that the cache is active, which means the identical objects are returned.
	 */
	@Test
	public void validateCache() {
		Country be1 = this.countryRepository.findByCode("BE");
		Country be2 = this.countryRepository.findByCode("BE");
		assertTrue(be1 == be2);
	}

	@Test
	public void checkForCache2kUsage() {
		Cache countries = this.cacheManager.getCache("countriesViaCacheManager");
		assertTrue(countries.getNativeCache() instanceof org.cache2k.Cache);
	}

	@Test
	public void loaderWorks() {
		Country be1 = this.loadingCountryRepository.findByCode("BE");
		Country be2 = this.loadingCountryRepository.findByCode("BE");
		assertTrue(be1 == be2);
		org.cache2k.Cache cache = org.cache2k.CacheManager.getInstance().getCache("loadingCountries");
		Country de = (Country) cache.get("DE");
		assertNotNull(de);
		assertEquals("DE", de.getCode());
	}

}
