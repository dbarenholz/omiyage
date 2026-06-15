import type { components } from './types-dto';
import type { User, Wish, WishList, WishLink, WishListSummary } from './types';

type Dto<T extends keyof components['schemas']> = components['schemas'][T];

export function mapUser(dto: Dto<'UserResponse'>): User {
    return {
        accountNumber: dto.accountNumber || '',
        displayName: dto.displayName || null,
        createdAt: dto.createdAt ? new Date(dto.createdAt) : new Date(),
    };
}

export function mapWishLink(dto: Dto<'WishLinkDto'>): WishLink {
    return {
        url: dto.url || '',
        label: dto.label || null,
    };
}

export function mapWish(dto: Dto<'WishResponse'>): Wish {
    return {
        id: dto.id || '',
        title: dto.title || '',
        description: dto.description || null,
        approximatePrice: dto.approximatePrice ?? null,
        currencyCode: dto.currencyCode || null,
        imageUrl: dto.imageUrl || null,
        links: dto.links?.map(mapWishLink) || [],
        tags: dto.tags || [],
        createdAt: dto.createdAt ? new Date(dto.createdAt) : new Date(),
        updatedAt: dto.updatedAt ? new Date(dto.updatedAt) : new Date(),
        claimed: dto.claimed ?? false,
        claimedByMe: dto.claimedByMe ?? false,
    };
}

export function mapWishList(dto: Dto<'WishListResponse'>): WishList {
    return {
        id: dto.id || '',
        title: dto.title || '',
        description: dto.description || null,
        shareId: dto.shareId || '',
        createdAt: dto.createdAt ? new Date(dto.createdAt) : new Date(),
        wishes: dto.wishes?.map(mapWish) || [],
    };
}

export function mapWishListSummary(dto: Dto<'WishListSummaryResponse'>): WishListSummary {
    return {
        id: dto.id || '',
        title: dto.title || '',
        description: dto.description || null,
        shareId: dto.shareId || '',
        createdAt: dto.createdAt ? new Date(dto.createdAt) : new Date(),
        updatedAt: dto.updatedAt ? new Date(dto.updatedAt) : new Date(),
        wishCount: dto.wishCount || 0,
    };
}
