import { test, expect } from '@playwright/test';

test.describe('Authentication Flows', () => {
    test('Signup Flow', async ({ page }) => {
        await page.goto('/signup');

        // Wait for generation to complete and show the save screen
        await expect(page.getByRole('heading', { name: '🔑 Save Your Number' })).toBeVisible();

        // Extract the generated account number
        const accountCode = page.locator('.id-display code');
        const accountNumber = await accountCode.textContent();
        expect(accountNumber).toBeTruthy();

        // Proceed to next step
        await page.getByRole('button', { name: "I've saved my number →" }).click();

        // Wait for name step
        await expect(page.getByRole('heading', { name: '👋 One more thing' })).toBeVisible();

        // Fill out display name
        await page.getByLabel('Display Name').fill('Test User');
        
        // Submit
        await page.getByRole('button', { name: 'Continue to dashboard →' }).click();

        // Verify redirection to dashboard
        await expect(page).toHaveURL(/\/dashboard/);
    });

    test('Login Flow', async ({ page }) => {
        // First we need a valid account to log in. We'll create one quickly.
        await page.goto('/signup');
        await expect(page.getByRole('heading', { name: '🔑 Save Your Number' })).toBeVisible();
        const accountCode = page.locator('.id-display code');
        const accountNumber = await accountCode.textContent();
        expect(accountNumber).toBeTruthy();

        await page.getByRole('button', { name: "I've saved my number →" }).click();
        await page.getByLabel('Display Name').fill('Login Tester');
        await page.getByRole('button', { name: 'Continue to dashboard →' }).click();
        await expect(page).toHaveURL(/\/dashboard/);

        // Clear local storage to simulate logout
        await page.evaluate(() => localStorage.clear());
        
        await page.goto('/login');

        // Test invalid login
        await page.getByLabel('Account Number').fill('1234');
        await page.getByRole('button', { name: 'Login' }).click();
        await expect(page.locator('.error-msg')).toContainText('Invalid account number format');

        // Test valid login
        if (accountNumber) {
            // Fill formats automatically, so we can just type the numbers or the formatted string
            await page.getByLabel('Account Number').fill(accountNumber);
            await page.getByRole('button', { name: 'Login' }).click();
            await expect(page).toHaveURL(/\/dashboard/);
        }
    });
});
