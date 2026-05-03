import { writable } from 'svelte/store';
import type { User } from './types';
import { getMe } from './api';

export const userStore = writable<User | null>(null);

export async function loadUser(): Promise<void> {
	const user = await getMe();
	userStore.set(user);
}
