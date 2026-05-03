export interface WishList {
id: string;
title: string;
description?: string;
/** Public share identifier — construct the share URL as /shared/<shareId> */
shareId: string;
createdAt: string;
updatedAt?: string;
wishCount?: number;
}

export interface WishLink {
id?: string;
url: string;
label?: string;
}

export interface Wish {
id: string;
title: string;
description?: string;
approximatePrice?: number;
currencyCode?: string;
imageUrl?: string;
tags: string[];
links: WishLink[];
/** null = viewer is owner or unauthenticated (claim status hidden to preserve surprise) */
claimed?: boolean | null;
/** true only when the current authenticated viewer is the claimer */
claimedByMe?: boolean | null;
createdAt: string;
updatedAt?: string;
}

export interface User {
/** Mullvad-style account number, formatted as XXXX-XXXX-XXXX-XXXX */
accountNumber: string;
displayName?: string;
}

export interface SharedListView {
id: string;
title: string;
description?: string;
shareId: string;
createdAt: string;
wishes: Wish[];
ownerDisplayName?: string;
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
