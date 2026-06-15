import '@testing-library/jest-dom/vitest';
import { setupServer } from 'msw/node';
import { handlers, resetDb } from './src/mocks/handlers';
import { beforeAll, afterEach, afterAll } from 'vitest';

export const server = setupServer(...handlers);

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => {
	server.resetHandlers();
	resetDb();
});
afterAll(() => server.close());
