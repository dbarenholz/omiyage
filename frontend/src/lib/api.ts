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

export interface ApiOptions {
	fetch?: typeof globalThis.fetch;
	csrfToken?: string;
}

let cachedCsrfToken = '';

async function request<T>(
	path: string,
	options: RequestInit = {},
	apiOptions: ApiOptions = {}
): Promise<T> {
	const fetchImpl = apiOptions.fetch || globalThis.fetch;
	const method = (options.method || 'GET').toUpperCase();
	const isMutating = ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method);

	const isFormData = browser && options.body instanceof FormData;
	const headers: Record<string, string> = {
		...(options.headers as Record<string, string>)
	};

	if (!isFormData) {
		headers['Content-Type'] = 'application/json';
	}

	if (isMutating) {
		let token = apiOptions.csrfToken;
		if (!token) {
			if (browser && cachedCsrfToken) {
				token = cachedCsrfToken;
			} else {
				try {
					const res = await fetchImpl(`${BASE}/auth/csrf`, { credentials: 'include' });
					const body = await res.json() as { token?: string };
					if (body?.token) {
						token = body.token;
						if (browser) cachedCsrfToken = token;
					}
				} catch { /* ignore */ }
			}
		}
		if (token) headers['X-XSRF-TOKEN'] = token;
	}

	const res = await fetchImpl(`${BASE}${path}`, {
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
export async function signup(displayName?: string, opts?: ApiOptions): Promise<User> {
	const res = await request<components['schemas']['SignupResponse']>('/auth/signup', {
		method: 'POST',
		body: JSON.stringify({ displayName })
	}, opts);
	return mapUser({ accountNumber: res.accountNumber, displayName: res.displayName, createdAt: new Date().toISOString() });
}

export async function login(accountNumber: string, opts?: ApiOptions): Promise<User> {
	if (browser) cachedCsrfToken = '';
	const res = await request<components['schemas']['UserResponse']>('/auth/login', {
		method: 'POST',
		body: JSON.stringify({ accountNumber })
	}, opts);
	return mapUser(res);
}

export async function logout(opts?: ApiOptions): Promise<void> {
	if (browser) cachedCsrfToken = '';
	return request<void>('/auth/logout', { method: 'POST' }, opts);
}

export async function updateMe(displayName: string, opts?: ApiOptions): Promise<User> {
	const res = await request<components['schemas']['UserResponse']>('/users/me', {
		method: 'PATCH',
		body: JSON.stringify({ displayName })
	}, opts);
	return mapUser(res);
}

export async function getMe(opts?: ApiOptions): Promise<User | null> {
	try {
		const res = await request<components['schemas']['UserResponse']>('/auth/me', {}, opts);
		return mapUser(res);
	} catch {
		return null;
	}
}

// Lists
export async function getLists(opts?: ApiOptions): Promise<WishListSummary[]> {
	const res = await request<components['schemas']['WishListSummaryResponse'][]>('/lists', {}, opts);
	return res.map(mapWishListSummary);
}

export async function createList(title: string, description?: string, opts?: ApiOptions): Promise<WishList> {
	const res = await request<components['schemas']['WishListResponse']>('/lists', {
		method: 'POST',
		body: JSON.stringify({ title, description })
	}, opts);
	return mapWishList(res);
}

export async function updateList(listId: string, data: Partial<WishList>, opts?: ApiOptions): Promise<WishList> {
	const res = await request<components['schemas']['WishListResponse']>(`/lists/${listId}`, {
		method: 'PATCH',
		body: JSON.stringify(data)
	}, opts);
	return mapWishList(res);
}

export async function deleteList(listId: string, opts?: ApiOptions): Promise<void> {
	return request<void>(`/lists/${listId}`, { method: 'DELETE' }, opts);
}

export async function getSharedList(shareId: string, opts?: ApiOptions): Promise<WishList> {
	const res = await request<components['schemas']['WishListResponse']>(`/lists/shared/${shareId}`, {}, opts);
	return mapWishList(res);
}

// Wishes
export async function getWishes(listId: string, opts?: ApiOptions): Promise<Wish[]> {
	const res = await request<components['schemas']['WishResponse'][]>(`/lists/${listId}/wishes`, {}, opts);
	return res.map(mapWish);
}

export async function createWish(listId: string, data: CreateWishData, opts?: ApiOptions): Promise<Wish> {
	const res = await request<components['schemas']['WishResponse']>(`/lists/${listId}/wishes`, {
		method: 'POST',
		body: JSON.stringify(data)
	}, opts);
	return mapWish(res);
}

export async function uploadWishImage(file: File, opts?: ApiOptions): Promise<string> {
	const formData = new FormData();
	formData.append('file', file);
	const res = await request<{ imageUrl: string }>('/uploads/images', {
		method: 'POST',
		body: formData
	}, opts);
	return res.imageUrl;
}

export async function updateWish(
	listId: string,
	wishId: string,
	data: Partial<CreateWishData>,
	opts?: ApiOptions
): Promise<Wish> {
	const res = await request<components['schemas']['WishResponse']>(`/lists/${listId}/wishes/${wishId}`, {
		method: 'PATCH',
		body: JSON.stringify(data)
	}, opts);
	return mapWish(res);
}

export async function deleteWish(listId: string, wishId: string, opts?: ApiOptions): Promise<void> {
	return request<void>(`/lists/${listId}/wishes/${wishId}`, { method: 'DELETE' }, opts);
}

// Claims
export async function claimWish(wishId: string, opts?: ApiOptions): Promise<void> {
	return request<void>(`/wishes/${wishId}/claim`, { method: 'POST' }, opts);
}

export async function unclaimWish(wishId: string, opts?: ApiOptions): Promise<void> {
	return request<void>(`/wishes/${wishId}/claim`, { method: 'DELETE' }, opts);
}
