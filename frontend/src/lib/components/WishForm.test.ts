import { describe, it, expect, vi } from 'vitest';
import { render, fireEvent, waitFor } from '@testing-library/svelte';
import '@testing-library/jest-dom/vitest';
import WishForm from './WishForm.svelte';

vi.mock('$lib/api', () => ({
	uploadWishImage: vi.fn().mockResolvedValue('http://example.com/uploaded.jpg')
}));

describe('WishForm', () => {
	it('renders empty form by default', () => {
		const { getByLabelText } = render(WishForm);
		expect(getByLabelText(/Title/i)).toHaveValue('');
		expect(getByLabelText(/Description/i)).toHaveValue('');
	});

	it('renders initial data', () => {
		const initial = {
			title: 'A Wish',
			approximatePrice: 10,
			currencyCode: 'USD',
			tags: ['test']
		};
		const { getByLabelText, getByRole } = render(WishForm, { props: { initial } });
		expect(getByLabelText(/Title/i)).toHaveValue('A Wish');
		expect(getByLabelText(/Price/i)).toHaveValue(10);
		expect(getByRole('combobox')).toHaveValue('USD');
	});

	it('shows validation error if title is empty on submit', async () => {
		const onsubmit = vi.fn();
		const { getByText, getByRole } = render(WishForm, { props: { onsubmit } });
		
		const form = document.querySelector('form');
		await fireEvent.submit(form!);
		
		await waitFor(() => {
			expect(getByText('Title is required')).toBeInTheDocument();
		});
		expect(onsubmit).not.toHaveBeenCalled();
	});

	it('calls onsubmit with correct data when valid', async () => {
		const onsubmit = vi.fn();
		const { getByLabelText, getByRole } = render(WishForm, { props: { onsubmit } });
		
		await fireEvent.input(getByLabelText(/Title/i), { target: { value: 'New Wish' } });
		await fireEvent.input(getByLabelText(/Price/i), { target: { value: '15.5' } });
		
		const form = document.querySelector('form');
		await fireEvent.submit(form!);
		
		expect(onsubmit).toHaveBeenCalledWith(expect.objectContaining({
			title: 'New Wish',
			approximatePrice: 15.5,
			currencyCode: 'EUR',
			tags: [],
			links: []
		}));
	});

	it('adds a tag', async () => {
		const { getByPlaceholderText, getByText, getByRole } = render(WishForm);
		const tagInput = getByPlaceholderText(/Add tag/i);
		
		await fireEvent.input(tagInput, { target: { value: 'cool-tag' } });
		await fireEvent.keyDown(tagInput, { key: 'Enter' });
		
		expect(getByText('cool-tag')).toBeInTheDocument();
	});

	it('adds a link', async () => {
		const { getByLabelText, getByPlaceholderText, getByText, getAllByText } = render(WishForm);
		const linkUrlInput = getByLabelText('Links');
		const linkLabelInput = getByPlaceholderText('Label (optional)');
		
		await fireEvent.input(linkUrlInput, { target: { value: 'example.com' } });
		await fireEvent.input(linkLabelInput, { target: { value: 'My Link' } });
		
		const addBtns = getAllByText('Add');
		// The second add button is for links
		await fireEvent.click(addBtns[1]!);
		
		expect(getByText('My Link')).toBeInTheDocument();
	});
});
