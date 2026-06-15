import { describe, it, expect, vi } from 'vitest';
import { render, fireEvent } from '@testing-library/svelte';
import '@testing-library/jest-dom/vitest';
import ListCard from './ListCard.svelte';

describe('ListCard', () => {
	const mockList = {
		id: 'l1',
		title: 'Test List',
		description: 'Test Desc',
		shareId: 's1',
		createdAt: new Date(),
		updatedAt: new Date(),
		wishCount: 0
	};

	it('renders list title and description', () => {
		const { getByText } = render(ListCard, { props: { list: mockList } });

		expect(getByText('Test List')).toBeInTheDocument();
		expect(getByText('Test Desc')).toBeInTheDocument();
		expect(getByText('0 wishes')).toBeInTheDocument();
	});

	it('calls onedit when edit button is clicked', async () => {
		const onedit = vi.fn();
		const { getByTitle } = render(ListCard, { props: { list: mockList, onedit } });

		const editBtn = getByTitle('Edit list');
		await fireEvent.click(editBtn);

		expect(onedit).toHaveBeenCalledWith(mockList);
	});

	it('calls ondelete when delete button is clicked', async () => {
		const ondelete = vi.fn();
		const { getByTitle } = render(ListCard, { props: { list: mockList, ondelete } });

		const deleteBtn = getByTitle('Delete list');
		await fireEvent.click(deleteBtn);

		expect(ondelete).toHaveBeenCalledWith(mockList.id);
	});
});
