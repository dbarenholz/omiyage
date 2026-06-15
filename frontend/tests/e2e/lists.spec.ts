import { test, expect } from '@playwright/test';

test.describe('Lists & Wishes Flows', () => {
    test.beforeEach(async ({ page }) => {
        // Create an account and login
        await page.goto('/signup');
        await expect(page.getByRole('heading', { name: '🔑 Save Your Number' })).toBeVisible();
        await page.getByRole('button', { name: "I've saved my number →" }).click();
        await page.getByLabel('Display Name').fill('List Tester');
        await page.getByRole('button', { name: 'Continue to dashboard →' }).click();
        await expect(page).toHaveURL(/\/dashboard/);

        // Create a list to use for testing
        await page.getByRole('button', { name: '+ New List' }).first().click();
        await expect(page.getByRole('heading', { name: 'New Omiyage' })).toBeVisible();
        await page.getByLabel('Title *').fill('My Test List');
        await page.getByRole('button', { name: 'Create' }).click();
        
        // Wait for list to appear, then click into it
        await expect(page.getByRole('heading', { name: 'My Test List' })).toBeVisible();
        
        // Click the invisible link covering the card
        await page.locator('.card-link').first().click();
        
        // Verify we navigated to the list page
        await expect(page.getByRole('heading', { name: 'My Test List' })).toBeVisible();
        await expect(page.getByRole('heading', { name: 'No wishes yet' })).toBeVisible();
    });

    test('Add, Edit, and Delete a Wish', async ({ page }) => {
        // --- ADD WISH ---
        await page.getByRole('button', { name: '+ Add Wish' }).first().click();
        await expect(page.getByRole('heading', { name: 'Add Wish' })).toBeVisible();

        // Fill out wish form
        await page.getByLabel('Title *').fill('New Laptop');
        await page.getByLabel('Description').fill('A really fast one');
        await page.locator('#wish-price').fill('1999.99'); // Using id because price row has complex label association

        await page.getByRole('button', { name: 'Save' }).click();

        // Verify wish is added
        await expect(page.getByRole('heading', { name: 'New Laptop' })).toBeVisible();
        await expect(page.getByText('A really fast one')).toBeVisible();
        await expect(page.getByText('$1,999.99').or(page.getByText('€1,999.99'))).toBeVisible(); // Depends on default currency, let's just check 1,999.99
        await expect(page.getByText('1,999.99')).toBeVisible();

        // --- EDIT WISH ---
        await page.locator('button[title="Edit"]').click();
        await expect(page.getByRole('heading', { name: 'Edit Wish' })).toBeVisible();

        await page.getByLabel('Title *').fill('Gaming Laptop');
        await page.locator('#wish-price').fill('2499.99');
        await page.getByRole('button', { name: 'Save' }).click();

        // Verify edits
        await expect(page.getByRole('heading', { name: 'Gaming Laptop' })).toBeVisible();
        await expect(page.getByRole('heading', { name: 'New Laptop' })).not.toBeVisible();
        await expect(page.getByText('2,499.99')).toBeVisible();

        // --- DELETE WISH ---
        // Automatically accept dialogs
        page.on('dialog', dialog => dialog.accept());

        await page.locator('button[title="Delete"]').click();

        // Verify deletion
        await expect(page.getByRole('heading', { name: 'Gaming Laptop' })).not.toBeVisible();
        await expect(page.getByRole('heading', { name: 'No wishes yet' })).toBeVisible();
    });
});
