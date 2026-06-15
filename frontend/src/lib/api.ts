import { browser } from '$app/environment';
import { goto } from '$app/navigation';
import { env } from '$env/dynamic/public';
import type { Wish, WishList, User, CreateWishData, WishListSummary } from './types';
import type { components } from './types-dto';
import { mapUser, mapWishList, mapWishListSummary, mapWish } from './mappers';

export class ApiError extends Error {
	status: number;

	constructor(status: number, message: string) {
		super(message);
		this.name = 'ApiError';
		this.status = status;
	}
}

const defaultApiUrl = browser
	? `${globalThis.location.protocol}//${globalThis.location.hostname}`
	: 'http://localhost';
const apiUrl = env.PUBLIC_API_URL || defaultApiUrl;
const apiPort = env.PUBLIC_API_PORT || '8080';
const apiPath = env.PUBLIC_API_PATH || '/api';

const BASE = apiPort ? `${apiUrl}:${apiPort}${apiPath}` : `${apiUrl}${apiPath}`;

let csrfTokenCache = '';

function getCsrfToken(): string {
	if (!browser) return '';
	if (csrfTokenCache) return csrfTokenCache;
	const match = /(?:^|;\s*)XSRF-TOKEN=([^;]+)/.exec(document.cookie);
	if (match) {
		let token = match[1] || '';
		csrfTokenCache = decodeURIComponent(token);
	}
	return csrfTokenCache;
}

async function ensureCsrfToken(): Promise<void> {
	if (!browser) return;
	const res = await fetch(`${BASE}/auth/csrf`, {
		credentials: 'include'
	});

	try {
		const body = await res.json() as { token?: string };
		if (body?.token) {
			csrfTokenCache = body.token;
		}
	} catch {
		// ignore
	}

	if (!csrfTokenCache) {
		csrfTokenCache = getCsrfToken();
	}
}

async function request<T>(
	path: string,
	options: RequestInit = {}
): Promise<T> {
	const method = (options.method || 'GET').toUpperCase();
	const isMutating = ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method);

	if (isMutating) {
		await ensureCsrfToken();
	}

	const isFormData = browser && options.body instanceof FormData;
	const headers: Record<string, string> = {
		...(options.headers as Record<string, string>)
	};

	if (!isFormData) {
		headers['Content-Type'] = 'application/json';
	}

	if (isMutating) {
		const token = getCsrfToken();
		if (token) headers['X-XSRF-TOKEN'] = token;
	}

	const res = await fetch(`${BASE}${path}`, {
		...options,
		credentials: 'include',
		headers
	});

	if (res.status === 401 && browser && !['/auth/me', '/auth/login', '/auth/signup'].includes(path)) {
		goto('/login');
		throw new ApiError(401, 'Unauthorized');
	}

	if (!res.ok) {
		let message = `HTTP ${res.status}`;
		try {
			const body = await res.json();
			message = body.message || body.error || message;
		} catch { /* ignore */ }
		throw new ApiError(res.status, message);
	}

	if (res.status === 204) return undefined as T;

	return res.json();
}

// Auth
export async function signup(displayName?: string): Promise<User> {
	const res = await request<components['schemas']['SignupResponse']>('/auth/signup', {
		method: 'POST',
		body: JSON.stringify({ displayName })
	});
	return mapUser({ accountNumber: res.accountNumber, displayName: res.displayName, createdAt: new Date().toISOString() });
}

export async function login(accountNumber: string): Promise<User> {
	const res = await request<components['schemas']['UserResponse']>('/auth/login', {
		method: 'POST',
		body: JSON.stringify({ accountNumber })
	});
	return mapUser(res);
}

export async function logout(): Promise<void> {
	return request<void>('/auth/logout', { method: 'POST' });
}

export async function updateMe(displayName: string): Promise<User> {
	const res = await request<components['schemas']['UserResponse']>('/users/me', {
		method: 'PATCH',
		body: JSON.stringify({ displayName })
	});
	return mapUser(res);
}

export async function getMe(): Promise<User | null> {
	try {
		const res = await request<components['schemas']['UserResponse']>('/auth/me');
		return mapUser(res);
	} catch {
		return null;
	}
}

// Lists
export async function getLists(): Promise<WishListSummary[]> {
	const res = await request<components['schemas']['WishListSummaryResponse'][]>('/lists');
	return res.map(mapWishListSummary);
}

export async function createList(title: string, description?: string): Promise<WishList> {
	const res = await request<components['schemas']['WishListResponse']>('/lists', {
		method: 'POST',
		body: JSON.stringify({ title, description })
	});
	return mapWishList(res);
}

export async function updateList(listId: string, data: Partial<WishList>): Promise<WishList> {
	const res = await request<components['schemas']['WishListResponse']>(`/lists/${listId}`, {
		method: 'PATCH',
		body: JSON.stringify(data)
	});
	return mapWishList(res);
}

export async function deleteList(listId: string): Promise<void> {
	return request<void>(`/lists/${listId}`, { method: 'DELETE' });
}

export async function getSharedList(shareId: string): Promise<WishList> {
	const res = await request<components['schemas']['WishListResponse']>(`/lists/shared/${shareId}`);
	return mapWishList(res);
}

// Wishes
export async function getWishes(listId: string): Promise<Wish[]> {
	const res = await request<components['schemas']['WishResponse'][]>(`/lists/${listId}/wishes`);
	return res.map(mapWish);
}

export async function createWish(listId: string, data: CreateWishData): Promise<Wish> {
	const res = await request<components['schemas']['WishResponse']>(`/lists/${listId}/wishes`, {
		method: 'POST',
		body: JSON.stringify(data)
	});
	return mapWish(res);
}

export async function uploadWishImage(file: File): Promise<string> {
	const formData = new FormData();
	formData.append('file', file);
	const res = await request<{ imageUrl: string }>('/uploads/images', {
		method: 'POST',
		body: formData
	});
	return res.imageUrl;
}

export async function updateWish(
	listId: string,
	wishId: string,
	data: Partial<CreateWishData>
): Promise<Wish> {
	const res = await request<components['schemas']['WishResponse']>(`/lists/${listId}/wishes/${wishId}`, {
		method: 'PATCH',
		body: JSON.stringify(data)
	});
	return mapWish(res);
}

export async function deleteWish(listId: string, wishId: string): Promise<void> {
	return request<void>(`/lists/${listId}/wishes/${wishId}`, { method: 'DELETE' });
}

// Claims
export async function claimWish(wishId: string): Promise<void> {
	return request<void>(`/wishes/${wishId}/claim`, { method: 'POST' });
}

export async function unclaimWish(wishId: string): Promise<void> {
	return request<void>(`/wishes/${wishId}/claim`, { method: 'DELETE' });
}
