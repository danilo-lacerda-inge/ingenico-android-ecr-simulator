# Coffee House product illustration brief

Create one square, edge-to-edge product illustration for each menu item in the
Coffee House demo. The set should feel like one small independent cafe's menu:
warm, friendly, contemporary, and easy to scan from a cashier screen.

## Shared visual direction

- Use a restrained editorial, flat-gouache illustration style with softly
  rounded geometry, subtle paper-like warmth, and clean dark espresso outlines.
- Keep each food or drink centered, large, and recognizable at a glance. Use a
  consistent camera angle, object scale, outline weight, and soft oval ground
  shadow for every item.
- Use a warm ivory canvas with a faint, low-contrast halo. Restrict accents to
  espresso brown, oat, muted sage, berry, citrus, and pastry gold.
- Use simple highlights and ingredient details rather than photo textures,
  gradients with strong contrast, extra props, lettering, logos, or borders.
- Keep the food illustration itself distinct for every product while retaining
  the same visual treatment and composition across the complete set.

## Product subjects

| Category | Product subjects |
|---|---|
| Hot drinks | Black Americano in a ceramic mug; spiced chai latte with pale foam; cappuccino with simple latte art; dark chocolate mocha in a ceramic mug. |
| Cold drinks | Berry smoothie with visible berries; iced cold brew with ice; pale green iced matcha latte; bright lemonade with a lemon garnish. |
| Salads | Caesar with leafy greens and parmesan; garden salad with mixed vegetables; Greek salad with tomato, feta, and olives; quinoa bowl with grains and roasted vegetables. |
| Snacks | Golden banana-bread slice; oat-and-seed granola bar; twisted golden soft pretzel; clear layered yogurt and fruit cup. |
| Sandwiches | Caprese panini with tomato and greens; chicken club with layered filling; egg salad sandwich; turkey and avocado sandwich. |
| Desserts | Dense chocolate brownie; carrot-cake slice with frosting; New York cheesecake slice; round lemon tart with a bright citrus center. |

## Assets

The 24 illustrations supplied in `docs/cardapio_crops_296x260.zip` are bundled
in `app/src/main/res/drawable-nodpi/product_*.png` and mapped to their catalog
items in `MainActivity.kt`. The archive tiles are ordered by category, four
products per row, matching the catalog's product order. Images load locally;
no network access is needed.
