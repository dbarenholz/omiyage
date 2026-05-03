<script lang="ts">
	import { createEventDispatcher } from 'svelte';
	import { uploadWishImage } from '$lib/api';
	import { currencyOptions } from '$lib/currencies';
	import type { CreateWishData, WishLink } from '$lib/types';

	export let initial: Partial<CreateWishData> = {};
	export let loading = false;

	const dispatch = createEventDispatcher<{ submit: CreateWishData; cancel: void }>();

	let title = initial.title || '';
	let description = initial.description || '';
	let priceStr = initial.approximatePrice != null ? String(initial.approximatePrice) : '';
	let currencyCode = (initial.currencyCode || 'EUR').toUpperCase();
	let imageUrl = initial.imageUrl || '';
	let tags: string[] = initial.tags ? [...initial.tags] : [];
	let links: WishLink[] = initial.links ? [...initial.links] : [];
	let tagInput = '';
	let newLinkUrl = '';
	let newLinkLabel = '';
	let titleTouched = false;
	let priceTouched = false;
	let imageUrlTouched = false;
	let linkUrlTouched = false;
	let linkAddError = '';
	let uploadLoading = false;
	let uploadError = '';
	let error = '';

	$: normalizedTitle = title.trim();
	$: parsedPrice = priceStr ? Number.parseFloat(priceStr) : undefined;
	$: titleError = titleTouched && !normalizedTitle ? 'Title is required' : '';
	$: priceError =
		priceTouched && priceStr && (parsedPrice == null || Number.isNaN(parsedPrice) || parsedPrice < 0)
			? 'Price must be a valid non-negative number'
			: '';
	$: imageUrlError =
		imageUrlTouched && imageUrl.trim() && !isValidUrl(imageUrl.trim())
			? 'Enter a valid image URL'
			: '';
	$: normalizedLinkInput = normalizeUrlInput(newLinkUrl);
	$: liveLinkError =
		linkUrlTouched && newLinkUrl.trim() && !normalizedLinkInput ? 'Enter a valid link URL' : '';

	function addTag() {
		const val = tagInput.trim().replace(/,+$/, '');
		if (val && !tags.includes(val)) {
			tags = [...tags, val];
		}
		tagInput = '';
	}

	function handleTagKeydown(e: KeyboardEvent) {
		if (e.key === 'Enter' || e.key === ',') {
			e.preventDefault();
			addTag();
		}
	}

	function handleTagBlur() {
		addTag();
	}

	function removeTag(tag: string) {
		tags = tags.filter((t) => t !== tag);
	}

	function addLink() {
		linkUrlTouched = true;
		linkAddError = '';

		if (!newLinkUrl.trim()) {
			linkAddError = 'Link URL is required';
			return;
		}

		if (!normalizedLinkInput) {
			linkAddError = 'Enter a valid link URL';
			return;
		}

		links = [...links, { url: normalizedLinkInput, label: newLinkLabel.trim() || undefined }];
		newLinkUrl = '';
		newLinkLabel = '';
		linkUrlTouched = false;
	}

	function removeLink(index: number) {
		links = links.filter((_, i) => i !== index);
	}

	function handleSubmit() {
		error = '';
		titleTouched = true;
		priceTouched = true;
		imageUrlTouched = true;

		if (titleError) {
			error = 'Title is required';
			return;
		}
		if (priceError) {
			error = 'Invalid price';
			return;
		}
		if (imageUrlError) {
			error = imageUrlError;
			return;
		}
		dispatch('submit', {
			title: normalizedTitle,
			description: description.trim() || undefined,
			approximatePrice: parsedPrice,
			currencyCode,
			imageUrl: imageUrl.trim() || undefined,
			tags,
			links
		});
	}

	function isValidUrl(value: string): boolean {
		try {
			new URL(value);
			return true;
		} catch {
			return false;
		}
	}

	function normalizeUrlInput(value: string): string | null {
		const trimmed = value.trim();
		if (!trimmed) return null;

		const withProtocol = /^https?:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`;
		return isValidUrl(withProtocol) ? withProtocol : null;
	}

	async function handleImageFileChange(event: Event) {
		uploadError = '';
		const target = event.currentTarget as HTMLInputElement;
		const file = target.files?.[0];
		if (!file) return;

		uploadLoading = true;
		try {
			imageUrl = await uploadWishImage(file);
		} catch (e: unknown) {
			uploadError = e instanceof Error ? e.message : 'Image upload failed';
		} finally {
			uploadLoading = false;
			target.value = '';
		}
	}
</script>

<form on:submit|preventDefault={handleSubmit}>
	<div class="form-group">
		<label for="wish-title">Title *</label>
		<input
			id="wish-title"
			bind:value={title}
			on:input={() => (titleTouched = true)}
			on:blur={() => (titleTouched = true)}
			class:field-invalid={!!titleError}
			class:field-valid={titleTouched && !titleError}
			placeholder="What do you wish for?"
			required
		/>
	</div>

	<div class="form-group">
		<label for="wish-desc">Description</label>
		<textarea id="wish-desc" bind:value={description} rows="3" placeholder="More details…"></textarea>
	</div>

	<div class="form-group">
		<label for="wish-price">Price</label>
		<div class="price-row">
			<input
				id="wish-price"
				bind:value={priceStr}
				type="number"
				min="0"
				step="0.01"
				on:input={() => (priceTouched = true)}
				on:blur={() => (priceTouched = true)}
				class:field-invalid={!!priceError}
				class:field-valid={priceTouched && !priceError}
				placeholder="0.00"
			/>
			<select bind:value={currencyCode} aria-label="Currency">
				{#each currencyOptions as currency}
					<option value={currency.code}>{currency.label}</option>
				{/each}
			</select>
		</div>
	</div>

	<div class="form-group">
		<label for="wish-image-url">Image URL</label>
		<input
			id="wish-image-url"
			bind:value={imageUrl}
			on:input={() => (imageUrlTouched = true)}
			on:blur={() => (imageUrlTouched = true)}
			class:field-invalid={!!imageUrlError}
			class:field-valid={imageUrlTouched && !imageUrlError}
			placeholder="https://..."
		/>
		{#if imageUrlError}
			<p class="error-msg">{imageUrlError}</p>
		{/if}
		<p class="field-hint">You can paste an image URL, or upload one below.</p>
	</div>

	<div class="form-group">
		<label for="wish-image-upload">Upload Image</label>
		<input
			id="wish-image-upload"
			type="file"
			accept="image/*"
			on:change={handleImageFileChange}
			disabled={uploadLoading || loading}
		/>
		{#if uploadLoading}
			<p class="field-hint">Uploading image…</p>
		{/if}
		{#if uploadError}
			<p class="error-msg">{uploadError}</p>
		{/if}
	</div>

	<div class="form-group">
		<label for="tag-input">Tags</label>
		<div class="tag-input-row">
			<input
				id="tag-input"
				bind:value={tagInput}
				on:keydown={handleTagKeydown}
				on:blur={handleTagBlur}
				placeholder="Add tag, press Enter or comma"
			/>
			<button type="button" class="btn-secondary add-btn" on:click={addTag}>Add</button>
		</div>
		{#if tags.length > 0}
			<div class="tag-list">
				{#each tags as tag}
					<span class="tag tag-cyan">
						{tag}
						<button type="button" class="remove-tag" on:click={() => removeTag(tag)}>✕</button>
					</span>
				{/each}
			</div>
		{/if}
	</div>

	<div class="form-group">
		<label for="link-url-input">Links</label>
		<div class="link-row">
			<input
				id="link-url-input"
				bind:value={newLinkUrl}
				on:input={() => {
					linkUrlTouched = true;
					linkAddError = '';
				}}
				on:blur={() => (linkUrlTouched = true)}
				class="link-url"
				class:field-invalid={!!(linkAddError || liveLinkError)}
				class:field-valid={linkUrlTouched && !!newLinkUrl.trim() && !(linkAddError || liveLinkError)}
				placeholder="https://..."
			/>
			<input bind:value={newLinkLabel} placeholder="Label (optional)" class="link-label" />
			<button type="button" class="btn-secondary add-btn" on:click={addLink}>Add</button>
		</div>
		{#if linkAddError || liveLinkError}
			<p class="error-msg">{linkAddError || liveLinkError}</p>
		{/if}
		{#if links.length > 0}
			<ul class="link-list">
				{#each links as link, i}
					<li class="link-item">
						<span class="link-text">{link.label || link.url}</span>
						<button type="button" class="remove-tag" on:click={() => removeLink(i)}>✕</button>
					</li>
				{/each}
			</ul>
		{/if}
	</div>

	{#if error}
		<p class="error-msg">{error}</p>
	{/if}

	<div class="form-actions">
		<button type="button" class="btn-ghost" on:click={() => dispatch('cancel')} disabled={loading}>
			Cancel
		</button>
		<button type="submit" class="btn-primary" disabled={loading}>
			{loading ? 'Saving…' : 'Save'}
		</button>
	</div>
</form>

<style lang="scss">
	textarea {
		resize: vertical;
	}

	.field-hint {
		font-size: 0.78rem;
		color: var(--text);
		margin-top: 0.3rem;
		opacity: 0.75;
	}

	.tag-input-row,
	.link-row {
		display: flex;
		gap: 0.5rem;
		align-items: center;
		flex-wrap: wrap;
	}

	.link-url { flex: 2; min-width: 0; }
	.link-label { flex: 1; min-width: 0; }

	.price-row {
		display: flex;
		gap: 0.5rem;
		align-items: center;
		width: 100%;

		input {
			flex: 1 1 8rem;
			min-width: 6.5rem;
			width: 0;
		}

		select {
			flex: 1 1 12rem;
			min-width: 0;
			width: 100%;
		}
	}

	@media (max-width: 520px) {
		.price-row {
			flex-wrap: wrap;

			input,
			select {
				flex: 1 1 100%;
				width: 100%;
			}
		}
	}

	.add-btn {
		white-space: nowrap;
		flex-shrink: 0;
	}

	.tag-list {
		display: flex;
		flex-wrap: wrap;
		gap: 0.35rem;
		margin-top: 0.5rem;
	}

	.tag {
		display: inline-flex;
		align-items: center;
		gap: 0.3rem;
	}

	.remove-tag {
		background: transparent;
		border: none;
		color: inherit;
		padding: 0;
		font-size: 0.7rem;
		line-height: 1;
		cursor: pointer;
		opacity: 0.7;
	}

	.remove-tag:hover {
		opacity: 1;
	}

	.link-list {
		list-style: none;
		margin-top: 0.5rem;
		display: flex;
		flex-direction: column;
		gap: 0.35rem;
	}

	.link-item {
		display: flex;
		align-items: center;
		justify-content: space-between;
		background: var(--surface-2);
		border-radius: var(--radius-sm);
		padding: 0.3rem 0.6rem;
		font-size: 0.82rem;
		color: var(--text);
	}

	.link-text {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		flex: 1;
		min-width: 0;
	}

	.form-actions {
		display: flex;
		justify-content: flex-end;
		gap: 0.75rem;
		margin-top: 1.25rem;
	}
</style>
