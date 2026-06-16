import { fail } from '@sveltejs/kit';
import { getLists, createList, updateList, deleteList } from '$lib/api';
import type { PageServerLoad, Actions } from './$types';

export const load: PageServerLoad = async ({ fetch }) => {
	const lists = await getLists({ fetch });
	return { lists };
};

export const actions: Actions = {
	create: async ({ request, fetch }) => {
		const data = await request.formData();
		const title = data.get('title')?.toString()?.trim();
		const description = data.get('description')?.toString()?.trim() || undefined;

		if (!title) return fail(400, { createError: 'Title is required', title, description });

		try {
			await createList(title, description, { fetch });
			return { createSuccess: true };
		} catch (e: any) {
			return fail(500, { createError: e.message, title, description });
		}
	},
	update: async ({ request, fetch }) => {
		const data = await request.formData();
		const id = data.get('id')?.toString();
		const title = data.get('title')?.toString()?.trim();
		const description = data.get('description')?.toString()?.trim() || undefined;

		if (!id || !title) return fail(400, { editError: 'Title is required', id, title, description });

		try {
			await updateList(id, { title, description }, { fetch });
			return { editSuccess: true };
		} catch (e: any) {
			return fail(500, { editError: e.message, id, title, description });
		}
	},
	delete: async ({ request, fetch }) => {
		const data = await request.formData();
		const id = data.get('id')?.toString();
		if (!id) return fail(400, { deleteError: 'Missing ID' });

		try {
			await deleteList(id, { fetch });
			return { deleteSuccess: true };
		} catch (e: any) {
			return fail(500, { deleteError: e.message });
		}
	}
};
