import { http, HttpResponse, delay } from 'msw';
import type { components } from '../lib/types-dto';

type UserResponse = components['schemas']['UserResponse'];
type WishListSummaryResponse = components['schemas']['WishListSummaryResponse'];
type WishListResponse = components['schemas']['WishListResponse'];
type WishResponse = components['schemas']['WishResponse'];

// In-memory db for testing
let currentUser: UserResponse | null = null;
let users: Record<string, UserResponse> = {}; // By account number
let lists: WishListSummaryResponse[] = [];
let wishes: Record<string, WishResponse[]> = {}; // listId -> WishResponse[]

export function resetDb() {
	currentUser = null;
	users = {};
	lists = [];
	wishes = {};
}

export function seedDb(customLists: WishListSummaryResponse[] = [], customWishes: Record<string, WishResponse[]> = {}, user: UserResponse | null = null) {
	lists = [...customLists];
	wishes = { ...customWishes };
	currentUser = user;
}

const csrfToken = 'mock-csrf-token';

export const handlers = [
	// CSRF
	http.get('*/api/auth/csrf', () => {
		return HttpResponse.json({ token: csrfToken }, {
			headers: {
				'Set-Cookie': `XSRF-TOKEN=${csrfToken}; Path=/; HttpOnly`
			}
		});
	}),

	// Auth
	http.post('*/api/auth/signup', async ({ request }) => {
		const body = await request.clone().json() as { displayName?: string };
		const acctNum = Math.random().toString().slice(2, 18);
		const user: UserResponse = {
			accountNumber: acctNum,
			displayName: body.displayName || undefined,
            createdAt: new Date().toISOString()
		};
		users[acctNum] = user;
		currentUser = user;
		return HttpResponse.json(user);
	}),

	http.post('*/api/auth/login', async ({ request }) => {
		const body = await request.clone().json() as { accountNumber: string };
		const user = users[body.accountNumber] || {
			accountNumber: body.accountNumber,
			displayName: 'Mock User',
            createdAt: new Date().toISOString()
		};
		currentUser = user;
		return HttpResponse.json(user);
	}),

	http.post('*/api/auth/logout', () => {
		currentUser = null;
		return new HttpResponse(null, { status: 204 });
	}),

	http.get('*/api/auth/me', () => {
		if (!currentUser) return new HttpResponse(null, { status: 401 });
		return HttpResponse.json(currentUser);
	}),

	// Users
	http.patch('*/api/users/me', async ({ request }) => {
		if (!currentUser) return new HttpResponse(null, { status: 401 });
		const body = await request.clone().json() as { displayName: string };
		currentUser.displayName = body.displayName;
		return HttpResponse.json(currentUser);
	}),

	// Lists
	http.get('*/api/lists', () => {
		if (!currentUser) return new HttpResponse(null, { status: 401 });
		return HttpResponse.json(lists);
	}),

	http.post('*/api/lists', async ({ request }) => {
		if (!currentUser) return new HttpResponse(null, { status: 401 });
		const body = await request.clone().json() as { title: string, description?: string };
		const newList: WishListSummaryResponse = {
			id: 'list-' + Date.now(),
			title: body.title,
			description: body.description || undefined,
			wishCount: 0,
			shareId: 'share-' + Date.now(),
			createdAt: new Date().toISOString(),
			updatedAt: new Date().toISOString()
		};
		lists.push(newList);
		wishes[newList.id || ''] = [];
		return HttpResponse.json({ ...newList, wishes: [] }); // create returns full WishListResponse
	}),

	http.get('*/api/lists/:listId', ({ params }) => {
		if (!currentUser) return new HttpResponse(null, { status: 401 });
		const list = lists.find(l => l.id === params.listId);
		if (!list) return new HttpResponse(null, { status: 404 });
		return HttpResponse.json({ ...list, wishes: wishes[list.id || ''] || [] });
	}),

	http.patch('*/api/lists/:listId', async ({ params, request }) => {
		if (!currentUser) return new HttpResponse(null, { status: 401 });
		const list = lists.find(l => l.id === params.listId);
		if (!list) return new HttpResponse(null, { status: 404 });
		const body = await request.clone().json() as Partial<WishListSummaryResponse>;
		if (body.title) list.title = body.title;
		if (body.description !== undefined) list.description = body.description;
		list.updatedAt = new Date().toISOString();
		return HttpResponse.json({ ...list, wishes: wishes[list.id || ''] || [] });
	}),

	http.delete('*/api/lists/:listId', ({ params }) => {
		if (!currentUser) return new HttpResponse(null, { status: 401 });
		lists = lists.filter(l => l.id !== params.listId);
		return new HttpResponse(null, { status: 204 });
	}),

	http.get('*/api/lists/shared/:shareId', ({ params }) => {
		const list = lists.find(l => l.shareId === params.shareId);
		if (!list) return new HttpResponse(null, { status: 404 });
		
		const listWishes = wishes[list.id || ''] || [];
		const view: WishListResponse = {
			...list,
			wishes: listWishes
		};
		return HttpResponse.json(view);
	}),

	// Wishes
	http.get('*/api/lists/:listId/wishes', ({ params }) => {
		if (!currentUser) return new HttpResponse(null, { status: 401 });
		return HttpResponse.json(wishes[params.listId as string] || []);
	}),

	http.post('*/api/lists/:listId/wishes', async ({ params, request }) => {
		if (!currentUser) return new HttpResponse(null, { status: 401 });
		const body = await request.clone().json() as Partial<WishResponse>;
		const listId = params.listId as string;
		if (!wishes[listId]) wishes[listId] = [];
		
		const newWish: WishResponse = {
			id: 'wish-' + Date.now(),
			title: body.title || 'Untitled',
			description: body.description || undefined,
			approximatePrice: body.approximatePrice || undefined,
			currencyCode: body.currencyCode || 'EUR',
			imageUrl: body.imageUrl || undefined,
			tags: body.tags || [],
			links: body.links || [],
			createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
			claimed: false,
			claimedByMe: false
		};
		wishes[listId].push(newWish);
		const list = lists.find(l => l.id === listId);
		if (list) list.wishCount = (list.wishCount || 0) + 1;
		return HttpResponse.json(newWish);
	}),

	http.patch('*/api/lists/:listId/wishes/:wishId', async ({ params, request }) => {
		if (!currentUser) return new HttpResponse(null, { status: 401 });
		const listId = params.listId as string;
		const wishId = params.wishId as string;
		const listWishes = wishes[listId] || [];
		const wish = listWishes.find(w => w.id === wishId);
		if (!wish) return new HttpResponse(null, { status: 404 });
		
		const body = await request.clone().json() as Partial<WishResponse>;
		Object.assign(wish, body);
        wish.updatedAt = new Date().toISOString();
		return HttpResponse.json(wish);
	}),

	http.delete('*/api/lists/:listId/wishes/:wishId', ({ params }) => {
		if (!currentUser) return new HttpResponse(null, { status: 401 });
		const listId = params.listId as string;
		const wishId = params.wishId as string;
		if (wishes[listId]) {
			wishes[listId] = wishes[listId].filter(w => w.id !== wishId);
			const list = lists.find(l => l.id === listId);
			if (list) list.wishCount = (list.wishCount || 1) - 1;
		}
		return new HttpResponse(null, { status: 204 });
	}),

	// Uploads
	http.post('*/api/uploads/images', async () => {
		if (!currentUser) return new HttpResponse(null, { status: 401 });
		await delay(100); // simulate upload
		return HttpResponse.json({ imageUrl: 'http://localhost:9000/omiyage-images/test-image.jpg' });
	}),

	// Claims
	http.post('*/api/wishes/:wishId/claim', ({ params }) => {
		if (!currentUser) return new HttpResponse(null, { status: 401 });
		const wishId = params.wishId as string;
		
		// Find wish across all lists
		for (const listWishes of Object.values(wishes)) {
			const wish = listWishes.find(w => w.id === wishId);
			if (wish) {
				wish.claimed = true;
				wish.claimedByMe = true;
				return new HttpResponse(null, { status: 204 });
			}
		}
		return new HttpResponse(null, { status: 404 });
	}),

	http.delete('*/api/wishes/:wishId/claim', ({ params }) => {
		if (!currentUser) return new HttpResponse(null, { status: 401 });
		const wishId = params.wishId as string;
		
		for (const listWishes of Object.values(wishes)) {
			const wish = listWishes.find(w => w.id === wishId);
			if (wish) {
				wish.claimed = false;
				wish.claimedByMe = false;
				return new HttpResponse(null, { status: 204 });
			}
		}
		return new HttpResponse(null, { status: 404 });
	})
];
