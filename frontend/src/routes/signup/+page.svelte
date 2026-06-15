<script lang="ts">
    import { goto } from "$app/navigation";
    import { onMount } from "svelte";
    import { login, signup, updateMe } from "$lib/api";
    import { userStore } from "$lib/stores";
    import { resolve } from "$app/paths";

    let step: "generate" | "save" | "name" = $state("generate");
    let generatedId = $state("");
    let displayName = $state("");
    let error = $state("");
    let loading = $state(false);
    let copied = $state(false);

    onMount(() => {
        void handleGenerate();
    });

    async function handleGenerate() {
        loading = true;
        error = "";
        try {
            const user = await signup();
            generatedId = user.accountNumber;
            userStore.set(user);
            step = "save";
        } catch (e: unknown) {
            error = e instanceof Error ? e.message : "Failed to create account";
        } finally {
            loading = false;
        }
    }

    async function handleCopy() {
        await navigator.clipboard.writeText(generatedId);
        copied = true;
        setTimeout(() => (copied = false), 2000);
    }

    function handleContinue() {
        error = "";
        const trimmedName = displayName.trim();
        if (!trimmedName) {
            error = "Display name is required";
            return;
        }

        loading = true;
        void (async () => {
            try {
                const loggedIn = await login(generatedId);
                const updated = await updateMe(trimmedName);
                userStore.set({
                    ...loggedIn,
                    displayName: updated.displayName,
                });
                goto(resolve("/dashboard"));
            } catch (e: unknown) {
                error =
                    e instanceof Error ? e.message : "Failed to finish setup";
            } finally {
                loading = false;
            }
        })();
    }
</script>

<svelte:head><title>Create Account — Omiyage</title></svelte:head>

<div class="auth-page">
    <div class="auth-card card">
        {#if step === "generate"}
            <h1>🎁 Create Account</h1>
            <p class="subtitle">
                We'll generate a unique account number. No email needed.
            </p>

            <div class="info-box">
                Accounts that are never used are <strong
                    >automatically deleted after 15 minutes</strong
                >. Make sure to log in after creating yours!
            </div>

            {#if error}
                <p class="error-msg">{error}</p>
            {/if}

            {#if loading}
                <div class="loading">Generating your account number…</div>
            {:else}
                <button class="btn-primary full-width" onclick={handleGenerate}
                    >Generate again</button
                >
            {/if}

            <div class="back-link">
                <a href={resolve("/login")}>Already have an account?</a>
            </div>
        {:else if step === "save"}
            <h1>🔑 Save Your Number</h1>

            <div class="warning-box">
                ⚠️ <strong>SAVE THIS NUMBER</strong> — it's the only way to log in.
                We can't recover it if you lose it. There is no password reset.
            </div>

            <div class="id-display">
                <code>{generatedId}</code>
                <button class="btn-ghost copy-btn" onclick={handleCopy}>
                    {copied ? "Copied!" : "Copy"}
                </button>
            </div>

            <div class="info-box">
                Accounts that are never used are <strong
                    >automatically deleted after 15 minutes</strong
                >. Make sure to log in!
            </div>
            <button
                class="btn-primary full-width"
                onclick={() => (step = "name")}
            >
                I've saved my number →
            </button>
        {:else}
            <h1>👋 One more thing</h1>
            <p class="subtitle">
                Add a display name so friends know it's you. Don't use your
                account number, or they can login to your account!
            </p>

            <div class="form-group">
                <label for="display-name">Display Name</label>
                <input
                    id="display-name"
                    bind:value={displayName}
                    placeholder="e.g. Alice"
                    maxlength="50"
                    required
                />
            </div>

            {#if error}
                <p class="error-msg">{error}</p>
            {/if}

            <button
                class="btn-primary full-width"
                onclick={handleContinue}
                disabled={loading || !displayName.trim()}
            >
                {loading ? "Setting up…" : "Continue to dashboard →"}
            </button>
        {/if}
    </div>
</div>

<style lang="scss">
    .auth-page {
        min-height: 100vh;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 1rem;
    }

    .auth-card {
        width: 100%;
        max-width: 420px;
    }

    h1 {
        font-size: 1.5rem;
        margin-bottom: 0.25rem;
        text-align: center;
    }

    .subtitle {
        text-align: center;
        color: var(--text);
        margin-bottom: 1.5rem;
        font-size: 0.9rem;
    }

    .warning-box {
        background: rgba(255, 204, 102, 0.15);
        border: 2px solid var(--accent);
        border-radius: var(--radius);
        padding: 0.9rem 1rem;
        color: var(--accent);
        font-size: 0.9rem;
        line-height: 1.5;
        margin-bottom: 1rem;
    }

    .info-box {
        background: rgba(92, 207, 230, 0.1);
        border: 1px solid rgba(92, 207, 230, 0.3);
        border-radius: var(--radius);
        padding: 0.75rem 1rem;
        color: var(--cyan);
        font-size: 0.85rem;
        line-height: 1.5;
        margin-bottom: 1.25rem;
    }

    .id-display {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        background: var(--surface-2);
        border-radius: var(--radius-sm);
        padding: 0.6rem 0.75rem;
        margin-bottom: 1rem;

        code {
            flex: 1;
            font-size: 0.9rem;
            color: var(--text-bright);
            word-break: break-all;
            font-family: "Courier New", monospace;
            letter-spacing: 0.05em;
        }
    }

    .copy-btn {
        white-space: nowrap;
        flex-shrink: 0;
        font-size: 0.8rem;
        padding: 0.3rem 0.6rem;
    }

    .full-width {
        width: 100%;
        padding: 0.65rem;
        font-size: 1rem;
    }

    .back-link {
        text-align: center;
        margin-top: 1rem;
        font-size: 0.85rem;
    }
</style>
