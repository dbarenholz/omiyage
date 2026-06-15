import { describe, it, expect, vi } from 'vitest';
import { render, fireEvent } from '@testing-library/svelte';
import '@testing-library/jest-dom/vitest';
import WishCard from './WishCard.svelte';
import type { Wish } from '$lib/types';

describe('WishCard', () => {
	const mockWish: Wish = {
		id: '1',
		title: 'Cool Gadget',
		description: 'Very cool indeed',
		approximatePrice: 49.99,
		currencyCode: 'EUR',
		imageUrl: 'http://example.com/image.png',
		links: [{ url: 'http://example.com', label: 'Buy here' }],
		tags: ['fun'],
		createdAt: new Date(),
		updatedAt: new Date(),
		claimed: false,
		claimedByMe: false
	};

	it('renders wish details', () => {
		const { getByText, getByRole } = render(WishCard, { props: { wish: mockWish } });

		expect(getByText('Cool Gadget')).toBeInTheDocument();
		expect(getByText('Very cool indeed')).toBeInTheDocument();
		// Price format depends on Intl but usually looks like €49.99
		expect(getByText(/€49\.99/)).toBeInTheDocument();
		expect(getByText('fun')).toBeInTheDocument();
		
		const img = getByRole('img') as HTMLImageElement;
		expect(img.src).toBe('http://example.com/image.png');
	});

	it('shows owner actions if isOwner is true', () => {
		const { getByTitle } = render(WishCard, { props: { wish: mockWish, isOwner: true } });
		
		expect(getByTitle('Edit')).toBeInTheDocument();
		expect(getByTitle('Delete')).toBeInTheDocument();
	});

	it('hides owner actions if isOwner is false', () => {
		const { queryByTitle } = render(WishCard, { props: { wish: mockWish, isOwner: false } });
		
		expect(queryByTitle('Edit')).not.toBeInTheDocument();
		expect(queryByTitle('Delete')).not.toBeInTheDocument();
	});

	it('calls onedit when edit is clicked', async () => {
		const onedit = vi.fn();
		const { getByTitle } = render(WishCard, { props: { wish: mockWish, isOwner: true, onedit } });
		
		await fireEvent.click(getByTitle('Edit'));
		expect(onedit).toHaveBeenCalledWith(mockWish);
	});

	it('shows claim button if canClaim is true and not owner', () => {
		const { getByText } = render(WishCard, { props: { wish: mockWish, canClaim: true, isOwner: false } });
		
		expect(getByText('Claim 🎁')).toBeInTheDocument();
	});

	it('shows unclaim button if claimedByMe is true', () => {
		const claimedWish = { ...mockWish, claimed: true, claimedByMe: true };
		const { getByText } = render(WishCard, { props: { wish: claimedWish, canClaim: true, isOwner: false } });
		
		expect(getByText('Unclaim')).toBeInTheDocument();
	});

	it('shows claimed badge if claimed by someone else', () => {
		const claimedWish = { ...mockWish, claimed: true, claimedByMe: false };
		const { getByText } = render(WishCard, { props: { wish: claimedWish, canClaim: true, isOwner: false } });
		
		expect(getByText('✓ Claimed')).toBeInTheDocument();
	});
});
