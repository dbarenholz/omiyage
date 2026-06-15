import { describe, it, expect, beforeEach, vi } from 'vitest';
import { resetDb, seedDb } from '../mocks/handlers';

vi.mock('$env/dynamic/public', () => ({
	env: {
		PUBLIC_API_URL: 'http://localhost',
		PUBLIC_API_PORT: '8080',
		PUBLIC_API_PATH: '/api'
	}
}));

import * as api from './api';

describe('API', () => {
	describe('Auth endpoints', () => {
		it('signup creates a user and returns it', async () => {
			const user = await api.signup('Alice');
			expect(user.displayName).toBe('Alice');
			expect(user.accountNumber).toBeDefined();
		});

		it('login returns the user', async () => {
			await api.signup('Bob');
			const loginResult = await api.login('123456'); // The mock login just returns a generic user or matching if we knew the acct num.
			// Let's just test it returns a user
			expect(loginResult).toBeDefined();
			expect(loginResult.accountNumber).toBe('123456');
		});

		it('logout successfully calls the endpoint', async () => {
			await api.signup('Alice'); // Set current user
			await expect(api.logout()).resolves.toBeUndefined(); // 204
		});

		it('getMe returns current user', async () => {
			const user = await api.signup('Alice');
			const me = await api.getMe();
			expect(me?.accountNumber).toEqual(user.accountNumber);
			expect(me?.displayName).toEqual(user.displayName);
		});

		it('updateMe updates display name', async () => {
			await api.signup('Alice');
			const updated = await api.updateMe('Alice Updated');
			expect(updated.displayName).toBe('Alice Updated');
		});
	});

	describe('List endpoints', () => {
		beforeEach(async () => {
			await api.signup('Alice');
		});

		it('createList creates a list', async () => {
			const list = await api.createList('My List', 'Desc');
			expect(list.title).toBe('My List');
			expect(list.description).toBe('Desc');
			expect(list.id).toBeDefined();
		});

		it('getLists returns all lists', async () => {
			await api.createList('List 1');
			await api.createList('List 2');
			const lists = await api.getLists();
			expect(lists.length).toBe(2);
		});

		it('updateList modifies the list', async () => {
			const list = await api.createList('Old Title');
			const updated = await api.updateList(list.id, { title: 'New Title' });
			expect(updated.title).toBe('New Title');
		});

		it('deleteList removes the list', async () => {
			const list = await api.createList('To Delete');
			await api.deleteList(list.id);
			const lists = await api.getLists();
			expect(lists).toHaveLength(0);
		});

		it('getSharedList returns the list and wishes', async () => {
			const list = await api.createList('Shared List');
			await api.createWish(list.id, { title: 'Wish 1', currencyCode: 'EUR', tags: [], links: [] });

			// Mock requires knowing shareId
			const lists = await api.getLists();
			const shareId = lists[0]!.shareId;

			const sharedView = await api.getSharedList(shareId);
			expect(sharedView.id).toBe(list.id);
			expect(sharedView.wishes).toHaveLength(1);
		});
	});

	describe('Wish endpoints', () => {
		let listId: string;
		beforeEach(async () => {
			await api.signup('Alice');
			const list = await api.createList('Wishes List');
			listId = list.id;
		});

		it('createWish creates a wish', async () => {
			const wish = await api.createWish(listId, { title: 'New Wish', approximatePrice: 10, currencyCode: 'USD', tags: [], links: [] });
			expect(wish.title).toBe('New Wish');
			expect(wish.approximatePrice).toBe(10);
		});

		it('getWishes returns list of wishes', async () => {
			await api.createWish(listId, { title: 'Wish 1', tags: [], links: [] });
			await api.createWish(listId, { title: 'Wish 2', tags: [], links: [] });
			const wishes = await api.getWishes(listId);
			expect(wishes).toHaveLength(2);
		});

		it('updateWish updates wish properties', async () => {
			const wish = await api.createWish(listId, { title: 'Old Wish', tags: [], links: [] });
			const updated = await api.updateWish(listId, wish.id, { title: 'Updated Wish', description: 'desc' });
			expect(updated.title).toBe('Updated Wish');
			expect(updated.description).toBe('desc');
		});

		it('deleteWish removes the wish', async () => {
			const wish = await api.createWish(listId, { title: 'To Delete', tags: [], links: [] });
			await api.deleteWish(listId, wish.id);
			const wishes = await api.getWishes(listId);
			expect(wishes).toHaveLength(0);
		});
	});

	describe('Uploads', () => {
		it('uploadWishImage uploads image and returns url', async () => {
			await api.signup('Alice');
			const file = new File([''], 'test.jpg', { type: 'image/jpeg' });
			const url = await api.uploadWishImage(file);
			expect(url).toContain('test-image.jpg');
		});
	});

	describe('Claims', () => {
		let wishId: string;
		beforeEach(async () => {
			await api.signup('Alice');
			const list = await api.createList('Wishes List');
			const wish = await api.createWish(list.id, { title: 'Claimable', tags: [], links: [] });
			wishId = wish.id;
		});

		it('claimWish claims the wish', async () => {
			await expect(api.claimWish(wishId)).resolves.toBeUndefined();

			// We can verify it was claimed by fetching the list (if we fetch shared view)
		});

		it('unclaimWish unclaims the wish', async () => {
			await api.claimWish(wishId);
			await expect(api.unclaimWish(wishId)).resolves.toBeUndefined();
		});
	});
});
