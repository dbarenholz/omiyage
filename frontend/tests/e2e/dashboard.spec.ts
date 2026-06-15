import { test, expect } from '@playwright/test';

test.describe('Dashboard Flows', () => {
    test.beforeEach(async ({ page }) => {
        // Create an account and login
        await page.goto('/signup');
        await expect(page.getByRole('heading', { name: '🔑 Save Your Number' })).toBeVisible();
        await page.getByRole('button', { name: "I've saved my number →" }).click();
        await page.getByLabel('Display Name').fill('Dashboard Tester');
        await page.getByRole('button', { name: 'Continue to dashboard →' }).click();
        await expect(page).toHaveURL(/\/dashboard/);
    });

    test('View Dashboard Empty State', async ({ page }) => {
        await expect(page.getByRole('heading', { name: 'My Wishlists' })).toBeVisible();
        await expect(page.getByRole('heading', { name: 'No wishlists yet' })).toBeVisible();
    });

    test('Create and Edit a List', async ({ page }) => {
        // Click New List button
        await page.getByRole('button', { name: '+ New List' }).first().click();

        // Wait for modal
        await expect(page.getByRole('heading', { name: 'New Omiyage' })).toBeVisible();

        // Fill out title
        await page.getByLabel('Title *').fill('My Birthday Wishlist');
        await page.getByLabel('Description').fill('Stuff I want for my birthday');

        // Submit
        await page.getByRole('button', { name: 'Create' }).click();

        // Verify the list appears
        await expect(page.getByRole('heading', { name: 'My Birthday Wishlist' })).toBeVisible();
        await expect(page.getByText('Stuff I want for my birthday')).toBeVisible();

        // Edit the list
        await page.locator('button[title="Edit list"]').click();

        await expect(page.getByRole('heading', { name: 'Edit Omiyage' })).toBeVisible();
        await page.getByLabel('Title *').fill('My Awesome Birthday');
        await page.getByRole('button', { name: 'Save' }).click();

        // Verify changes
        await expect(page.getByRole('heading', { name: 'My Awesome Birthday' })).toBeVisible();
        await expect(page.getByRole('heading', { name: 'My Birthday Wishlist' })).not.toBeVisible();
    });

    test('Delete a List', async ({ page }) => {
        // Create a list first
        await page.getByRole('button', { name: '+ New List' }).first().click();
        await page.getByLabel('Title *').fill('List to Delete');
        await page.getByRole('button', { name: 'Create' }).click();

        await expect(page.getByRole('heading', { name: 'List to Delete' })).toBeVisible();

        // Automatically accept dialogs
        page.on('dialog', dialog => dialog.accept());

        // Delete the list
        await page.locator('button[title="Delete list"]').click();

        // Verify deletion
        await expect(page.getByRole('heading', { name: 'List to Delete' })).not.toBeVisible();
        await expect(page.getByRole('heading', { name: 'No wishlists yet' })).toBeVisible();
    });
});
