import { data } from 'currency-codes';

export interface CurrencyOption {
	code: string;
	label: string;
}

const DUTCH_PRIORITY_CODES = [
	'EUR',
	'JPY',
	'USD',
	'GBP',
	'CHF',
	'DKK',
	'NOK',
	'SEK',
	'CAD',
	'AUD',
	'SGD',
	'CNY',
	'HKD',
	'TWD',
	'KRW',
	'INR',
	'THB',
	'AED',
	'TRY',
	'PLN',
	'HUF',
	'CZK',
	'RON'
];

const priorityIndex = new Map(DUTCH_PRIORITY_CODES.map((code, index) => [code, index]));

const uniqueCurrencies = Array.from(new Map(data.map((item) => [item.code, item])).values());

export const currencyOptions: CurrencyOption[] = uniqueCurrencies
	.map((currency) => ({
		code: currency.code,
		label: `${currency.code} - ${currency.currency}`
	}))
	.sort((a, b) => {
		const aPriority = priorityIndex.get(a.code);
		const bPriority = priorityIndex.get(b.code);

		if (aPriority != null && bPriority != null) return aPriority - bPriority;
		if (aPriority != null) return -1;
		if (bPriority != null) return 1;

		return a.label.localeCompare(b.label, 'en');
	});
