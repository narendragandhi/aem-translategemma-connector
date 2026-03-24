const { test, expect } = require('@playwright/test');

/**
 * Playwright E2E tests for the AEM TranslateGemma UI.
 * This validates that our "Gemma Insight" badges correctly overlay 
 * on the AEM Sites Touch UI list items.
 */
test.describe('AEM TranslateGemma UI Validation', () => {

    test.beforeEach(async ({ page }) => {
        // Mock the Audit API response to ensure consistent UI for testing
        await page.route('**/bin/translation-gemma/audit*', async route => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify([
                    {
                        path: '/content/example/en/master-page',
                        sentiment: {
                            sentiment: 'POSITIVE',
                            confidence: 0.95,
                            reasoning: 'Gemma logic: This content is overwhelmingly positive and matches brand tone.'
                        }
                    },
                    {
                        path: '/content/example/fr/live-copy',
                        sentiment: {
                            sentiment: 'NEUTRAL',
                            confidence: 0.65,
                            reasoning: 'Gemma logic: Some ambiguity in translation; requires author review.'
                        }
                    }
                ])
            });
        });

        // Navigate to the AEM Sites Console (Mocked/Proxy)
        await page.goto('/sites.html/content/example');
    });

    test('should display "Gemma: POSITIVE" badge for English master page', async ({ page }) => {
        const item = page.locator('.foundation-collection-item[data-foundation-collection-item-id="/content/example/en/master-page"]');
        await expect(item).toBeVisible();

        const badge = item.locator('.gemma-insight-badge');
        await expect(badge).toBeVisible();
        await expect(badge).toContainText('Gemma: POSITIVE (95%)');
        
        // Assert on the green color for high confidence
        const backgroundColor = await badge.evaluate(el => window.getComputedStyle(el).backgroundColor);
        expect(backgroundColor).toBe('rgb(40, 167, 69)'); // matches #28a745
    });

    test('should display "Gemma: NEUTRAL" badge with yellow color for Live Copy', async ({ page }) => {
        const item = page.locator('.foundation-collection-item[data-foundation-collection-item-id="/content/example/fr/live-copy"]');
        await expect(item).toBeVisible();

        const badge = item.locator('.gemma-insight-badge');
        await expect(badge).toBeVisible();
        await expect(badge).toContainText('Gemma: NEUTRAL (65%)');
        
        // Assert on the yellow color for medium confidence
        const backgroundColor = await badge.evaluate(el => window.getComputedStyle(el).backgroundColor);
        expect(backgroundColor).toBe('rgb(255, 193, 7)'); // matches #ffc107
    });

    test('should show reasoning in tooltip when hovering over badge', async ({ page }) => {
        const badge = page.locator('.gemma-insight-badge').first();
        await badge.hover();
        
        const tooltip = await badge.getAttribute('title');
        expect(tooltip).toContain('Gemma logic: This content is overwhelmingly positive');
    });
});
