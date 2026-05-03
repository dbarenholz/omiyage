import { browser } from '$app/environment';
import { goto } from '$app/navigation';
import { env } from '$env/dynamic/public';
import type { Wish, WishList, User, SharedListView, CreateWishData } from './types';

export class ApiError extends Error {
status: number;

constructor(status: number, message: string) {
super(message);
this.name = 'ApiError';
this.status = status;
}
}

// Build the API base URL from its three independent env vars:
//   PUBLIC_API_URL  – scheme + host, e.g. "http://localhost" or "https://api.example.com"
//   PUBLIC_API_PORT – port number,   e.g. "8080" (omit for standard 80/443)
//   PUBLIC_API_PATH – base path,     e.g. "/api" (default) or "/omiyage/api"
// If PUBLIC_API_URL is not set, use the same host the frontend is loaded from.
// This avoids cross-site cookie issues when opening the app via 127.0.0.1, LAN IPs, etc.
const defaultApiUrl = browser
	? `${globalThis.location.protocol}//${globalThis.location.hostname}`
	: 'http://localhost';
const apiUrl  = env.PUBLIC_API_URL  || defaultApiUrl;
const apiPort = env.PUBLIC_API_PORT || '8080';
const apiPath = env.PUBLIC_API_PATH || '/api';

const BASE = apiPort ? `${apiUrl}:${apiPort}${apiPath}` : `${apiUrl}${apiPath}`;

let csrfTokenCache = '';

function getCsrfToken(): string {
if (!browser) return '';
if (csrfTokenCache) return csrfTokenCache;
const match = /(?:^|;\s*)XSRF-TOKEN=([^;]+)/.exec(document.cookie);
if (match) {
csrfTokenCache = decodeURIComponent(match[1]);
}
return csrfTokenCache;
}

async function ensureCsrfToken(): Promise<void> {
if (!browser) return;
const res = await fetch(`${BASE}/auth/csrf`, {
credentials: 'include'
});

// Fallback for cross-host local dev (e.g., 127.0.0.1 -> localhost):
// browser may send cookie but JS cannot read it from document.cookie.
try {
const body = await res.json() as { token?: string };
if (body?.token) {
csrfTokenCache = body.token;
}
} catch {
// ignore parse issues; cookie-based token may still exist
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

if (res.status === 401 && browser) {
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
return request<User>('/auth/signup', {
method: 'POST',
body: JSON.stringify({ displayName })
});
}

export async function login(accountNumber: string): Promise<User> {
return request<User>('/auth/login', {
method: 'POST',
body: JSON.stringify({ accountNumber })
});
}

export async function logout(): Promise<void> {
return request<void>('/auth/logout', { method: 'POST' });
}

export async function updateMe(displayName: string): Promise<User> {
return request<User>('/users/me', {
method: 'PATCH',
body: JSON.stringify({ displayName })
});
}

export async function getMe(): Promise<User | null> {
try {
return await request<User>('/auth/me');
} catch {
return null;
}
}

// Lists
export async function getLists(): Promise<WishList[]> {
return request<WishList[]>('/lists');
}

export async function createList(title: string, description?: string): Promise<WishList> {
return request<WishList>('/lists', {
method: 'POST',
body: JSON.stringify({ title, description })
});
}

export async function updateList(listId: string, data: Partial<WishList>): Promise<WishList> {
return request<WishList>(`/lists/${listId}`, {
method: 'PATCH',
body: JSON.stringify(data)
});
}

export async function deleteList(listId: string): Promise<void> {
return request<void>(`/lists/${listId}`, { method: 'DELETE' });
}

export async function getSharedList(shareId: string): Promise<SharedListView> {
return request<SharedListView>(`/lists/shared/${shareId}`);
}

// Wishes
export async function getWishes(listId: string): Promise<Wish[]> {
return request<Wish[]>(`/lists/${listId}/wishes`);
}

export async function createWish(listId: string, data: CreateWishData): Promise<Wish> {
return request<Wish>(`/lists/${listId}/wishes`, {
method: 'POST',
body: JSON.stringify(data)
});
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
return request<Wish>(`/lists/${listId}/wishes/${wishId}`, {
method: 'PATCH',
body: JSON.stringify(data)
});
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
