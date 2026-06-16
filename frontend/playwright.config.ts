import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
    // Adjust this path based on where you want your E2E tests
    testDir: './tests/e2e',
    fullyParallel: true,
    forbidOnly: !!process.env.CI,
    retries: process.env.CI ? 2 : 0,
    workers: process.env.CI ? 1 : undefined,
    reporter: 'html',

    use: {
        // Port where your SvelteKit preview server runs
        baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:4173',
        trace: 'on-first-retry',
    },

    projects: [
        {
            name: 'chromium',
            use: { ...devices['Desktop Chrome'] },
        },
        {
            name: 'firefox',
            use: { ...devices['Desktop Firefox'] },
        },
    ],

    // Starts SvelteKit before tests run. 
    // You must spin up your Spring Docker Compose independently before running tests.
    webServer: process.env.PLAYWRIGHT_SKIP_WEBSERVER ? undefined : {
        command: 'bun run build && bun run preview',
        url: 'http://localhost:4173',
        reuseExistingServer: !process.env.CI,
    },
});