import { fail, error } from '@sveltejs/kit';
import { getLists, getWishes, createWish, updateWish, deleteWish, updateList } from '$lib/api';
import type { PageServerLoad, Actions } from './$types';

export const load: PageServerLoad = async ({ params, fetch }) => {
	const listId = params.listId;
	try {
		const [allLists, wishes] = await Promise.all([
			getLists({ fetch }),
			getWishes(listId, { fetch })
		]);
		const wishList = allLists.find((l) => l.id === listId);
		if (!wishList) throw error(404, 'List not found');
		
		return { wishList, wishes, listId };
	} catch (e: any) {
		throw error(500, e.message);
	}
};

export const actions: Actions = {
	createWish: async ({ request, params, fetch }) => {
		const data = await request.formData();
		const title = data.get('title')?.toString()?.trim();
		const description = data.get('description')?.toString()?.trim() || undefined;
		const priceStr = data.get('approximatePrice')?.toString();
		const currencyCode = data.get('currencyCode')?.toString() || 'EUR';
		const imageUrl = data.get('imageUrl')?.toString()?.trim() || undefined;
		const tagsStr = data.get('tags')?.toString();
		const linksStr = data.get('links')?.toString();

		if (!title) return fail(400, { createWishError: 'Title is required', ...Object.fromEntries(data) });

		const tags = tagsStr ? JSON.parse(tagsStr) : [];
		const links = linksStr ? JSON.parse(linksStr) : [];
		const approximatePrice = priceStr ? Number.parseFloat(priceStr) : undefined;

		try {
			await createWish(params.listId, {
				title,
				description,
				approximatePrice,
				currencyCode,
				imageUrl,
				tags,
				links
			}, { fetch });
			return { createWishSuccess: true };
		} catch (e: any) {
			return fail(500, { createWishError: e.message, ...Object.fromEntries(data) });
		}
	},
	updateWish: async ({ request, params, fetch }) => {
		const data = await request.formData();
		const id = data.get('id')?.toString();
		const title = data.get('title')?.toString()?.trim();
		const description = data.get('description')?.toString()?.trim() || undefined;
		const priceStr = data.get('approximatePrice')?.toString();
		const currencyCode = data.get('currencyCode')?.toString() || 'EUR';
		const imageUrl = data.get('imageUrl')?.toString()?.trim() || undefined;
		const tagsStr = data.get('tags')?.toString();
		const linksStr = data.get('links')?.toString();

		if (!id || !title) return fail(400, { updateWishError: 'Title is required', ...Object.fromEntries(data) });

		const tags = tagsStr ? JSON.parse(tagsStr) : [];
		const links = linksStr ? JSON.parse(linksStr) : [];
		const approximatePrice = priceStr && priceStr !== 'null' ? Number.parseFloat(priceStr) : undefined;

		try {
			await updateWish(params.listId, id, {
				title,
				description,
				approximatePrice,
				currencyCode,
				imageUrl,
				tags,
				links
			}, { fetch });
			return { updateWishSuccess: true };
		} catch (e: any) {
			return fail(500, { updateWishError: e.message, ...Object.fromEntries(data) });
		}
	},
	deleteWish: async ({ request, params, fetch }) => {
		const data = await request.formData();
		const id = data.get('id')?.toString();
		if (!id) return fail(400, { deleteWishError: 'Missing ID' });

		try {
			await deleteWish(params.listId, id, { fetch });
			return { deleteWishSuccess: true };
		} catch (e: any) {
			return fail(500, { deleteWishError: e.message });
		}
	},
	updateList: async ({ request, params, fetch }) => {
		const data = await request.formData();
		const title = data.get('title')?.toString()?.trim();
		const description = data.get('description')?.toString()?.trim() || undefined;

		if (!title) return fail(400, { updateListError: 'Title is required', ...Object.fromEntries(data) });

		try {
			await updateList(params.listId, { title, description }, { fetch });
			return { updateListSuccess: true };
		} catch (e: any) {
			return fail(500, { updateListError: e.message, ...Object.fromEntries(data) });
		}
	}
};
