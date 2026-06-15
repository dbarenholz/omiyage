import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, fireEvent } from '@testing-library/svelte';
import '@testing-library/jest-dom/vitest';
import NavBar from './NavBar.svelte';
import { userStore } from '$lib/stores';

// Mock $app/navigation and $app/paths
vi.mock('$app/navigation', () => ({
	goto: vi.fn()
}));
vi.mock('$app/paths', () => ({
	resolve: (path: string) => path
}));
vi.mock('$lib/api', () => ({
	logout: vi.fn().mockResolvedValue(undefined),
	updateMe: vi.fn((name: string) => Promise.resolve({ id: '1', displayName: name }))
}));

describe('NavBar', () => {
	beforeEach(() => {
		userStore.set({
			accountNumber: '123',
			displayName: 'Alice',
			createdAt: new Date()
		});
		vi.clearAllMocks();
	});

	it('renders user display name', () => {
		const { getByText } = render(NavBar);
		expect(getByText('Alice')).toBeInTheDocument();
	});

	it('opens display name modal on click', async () => {
		const { getByTitle, getByText } = render(NavBar);
		const editBtn = getByTitle('Edit display name');
		await fireEvent.click(editBtn);
		
		expect(getByText('Update Display Name')).toBeInTheDocument();
	});

	it('logs out on click', async () => {
		const { getByText } = render(NavBar);
		const logoutBtn = getByText('Logout');
		await fireEvent.click(logoutBtn);
		
		const { logout } = await import('$lib/api');
		expect(logout).toHaveBeenCalled();
	});
});
