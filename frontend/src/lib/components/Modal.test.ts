import { describe, it, expect, vi } from 'vitest';
import { render, fireEvent } from '@testing-library/svelte';
import '@testing-library/jest-dom/vitest';
import Modal from './Modal.svelte';
import { tick } from 'svelte';

describe('Modal', () => {
	it('does not render when open is false', () => {
		const { queryByRole } = render(Modal, { props: { open: false, title: 'Test Modal' } });
		expect(queryByRole('dialog')).not.toBeInTheDocument();
	});

	it('renders when open is true', () => {
		const { getByRole, getByText } = render(Modal, { props: { open: true, title: 'Test Modal' } });
		expect(getByRole('dialog')).toBeInTheDocument();
		expect(getByText('Test Modal')).toBeInTheDocument();
	});

	it('calls onclose when close button is clicked', async () => {
		const onclose = vi.fn();
		const { getByLabelText } = render(Modal, { props: { open: true, title: 'Test', onclose } });
		
		await fireEvent.click(getByLabelText('Close'));
		expect(onclose).toHaveBeenCalled();
	});

	it('calls onclose when overlay is clicked', async () => {
		const onclose = vi.fn();
		const { getByRole } = render(Modal, { props: { open: true, title: 'Test', onclose } });
		
		// The overlay is the presentation role element
		const overlay = getByRole('presentation');
		await fireEvent.click(overlay);
		
		expect(onclose).toHaveBeenCalled();
	});

	it('calls onclose when escape is pressed', async () => {
		const onclose = vi.fn();
		render(Modal, { props: { open: true, title: 'Test', onclose } });
		
		await fireEvent.keyDown(window, { key: 'Escape' });
		
		expect(onclose).toHaveBeenCalled();
	});
});
