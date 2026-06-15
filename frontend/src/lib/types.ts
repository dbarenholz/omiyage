export interface User {
    accountNumber: string;
    displayName: string | null;
    createdAt: Date;
}

export interface WishLink {
    url: string;
    label: string | null;
}

export interface Wish {
    id: string;
    title: string;
    description: string | null;
    approximatePrice: number | null;
    currencyCode: string | null;
    imageUrl: string | null;
    links: WishLink[];
    tags: string[];
    createdAt: Date;
    updatedAt: Date;

    // API Flags
    claimed: boolean;
    claimedByMe: boolean;

    // UI State Flags
    isClaiming?: boolean;
    isUnclaiming?: boolean;
}

export interface WishList {
    id: string;
    title: string;
    description: string | null;
    shareId: string;
    createdAt: Date;
    wishes: Wish[];
}

export interface WishListSummary {
    id: string;
    title: string;
    description: string | null;
    shareId: string;
    createdAt: Date;
    updatedAt: Date;
    wishCount: number;
}

export interface CreateWishData {
    title: string;
    description?: string;
    approximatePrice?: number;
    currencyCode?: string;
    imageUrl?: string;
    tags: string[];
    links: WishLink[];
}
