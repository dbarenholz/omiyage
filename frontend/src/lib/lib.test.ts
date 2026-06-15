import { describe, it, expect, vi } from 'vitest';
import { currencyOptions } from './currencies';
import { userStore, loadUser } from './stores';
import { get } from 'svelte/store';
import * as api from './api';

describe('currencies', () => {
	it('exports currencyOptions with code and label', () => {
		expect(currencyOptions.length).toBeGreaterThan(0);
		expect(currencyOptions[0]?.code).toBeDefined();
		expect(currencyOptions[0]?.label).toBeDefined();
	});

	it('prioritizes EUR, USD, GBP, JPY', () => {
		const topCodes = currencyOptions.slice(0, 4).map(c => c.code);
		expect(topCodes).toContain('EUR');
		expect(topCodes).toContain('USD');
		expect(topCodes).toContain('GBP');
		expect(topCodes).toContain('JPY');
	});
});

vi.mock('./api', () => ({
	getMe: vi.fn()
}));

describe('stores', () => {
	it('loadUser fetches user and sets store', async () => {
		const mockUser = { accountNumber: '123', displayName: 'A', createdAt: new Date() };
		vi.mocked(api.getMe).mockResolvedValue(mockUser);
		
		await loadUser();
		
		expect(get(userStore)).toEqual(mockUser);
	});
});
