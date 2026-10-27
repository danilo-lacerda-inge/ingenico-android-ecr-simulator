# AXIUM CX9000 on-device UI/UX guidelines

## Scope and evidence

This document extracts the **application UI shown on the CX9000 screens** in the supplied terminal images and the [AXIUM CX series video](https://youtu.be/mFJ_iO0bVYo?si=a3vDxZCEaSxuCoqd). It is not a description of the Ingenico marketing website, the terminal's physical design, or a claim that the pictured coffee-shop app is an official Ingenico application.

The product page describes an 8-inch HD touchscreen and a 15.6-inch high-resolution multitouchscreen. The examples below refer to the **on-device POS app screens**; do not infer an exact device resolution from the product renderings. The video and images show a branded coffee-shop demo, so treat its logo, imagery, and exact colors as application-specific.

**Evidence levels:** layout and visual relationships are directly observable; hex colors are approximate visual estimates from the screen images; font family and dp measurements are not published and are implementation guidance only.

## Visual direction

- **Dark, focused canvas:** use deep navy as the primary work surface so product imagery, totals, and key actions stand out.
- **Bright, restrained accents:** use cyan/teal for navigation and supporting actions, and mint green for the primary progression or selected state.
- **Commerce-first hierarchy:** product choices and the amount due are the visual focus; secondary navigation and utility icons remain quiet.
- **Soft geometry:** favor rounded controls and product tiles over sharp-edged, heavily outlined panels.
- **Branding is contextual:** the COFFEE HOUSE wordmark and coffee photography belong to the example app. Keep app branding configurable rather than treating either as a CX9000 system element.

## Color palette

Approximate values below are sampled by visual estimation from the supplied screen images; use them as a starting point and tune against the actual target display.

| Role | Approximate token | Usage |
|---|---|---|
| Main canvas | `#09265C` | Full-screen background and large work areas |
| Deep surface | `#071D49` | Emphasized panels, selected areas, and dark overlays |
| Raised blue | `#123D78` | Basket/table regions and secondary panels |
| Cyan | `#37B9D2` | Header bars, active navigation, and secondary actions |
| Teal | `#36A9B8` | Supporting buttons, filters, and selected controls |
| Mint | `#57C99B` | Primary actions and positive/selected states |
| White | `#FFFFFF` | High-priority text and strong contrast on dark surfaces |
| Soft white | `#EAF0F5` | Secondary text, icons, and light product tiles |
| Muted blue | `#A9BED7` | Less prominent labels and supporting information |

### Color use

- Keep the main background predominantly navy; reserve bright accents for actionable or selected elements.
- Use **mint consistently for the main forward action** (for example, adding or confirming an order). Do not use cyan and mint interchangeably for the same action level on one screen.
- Use cyan/teal for secondary navigation, tabs, and lower-emphasis actions.
- Light product tiles in the catalog are a deliberate contrast against the dark page. Keep their labels and imagery legible without adding strong borders.
- Do not rely on color alone for selection, errors, or status. Pair it with text, icons, or a clear selected shape.
- Verify text contrast and color meaning for the deployed market and app theme; the pictured color values are not an accessibility certification.

## Typography

The screenshots suggest a clean, compact sans-serif UI. The exact typeface is not identifiable from the supplied material. Use the platform sans-serif (Android Roboto) or the product app's existing sans-serif; avoid introducing a decorative font for functional content.

| Text role | Suggested size | Weight and treatment |
|---|---:|---|
| Screen title / section heading | 20–24sp | Medium or semibold |
| Product name / key amount | 18–22sp | Semibold; use the largest emphasis for the current total |
| Button label / category | 14–16sp | Medium; concise labels |
| Row label / quantity / price | 12–14sp | Regular or medium |
| Helper text / status | 11–12sp | Regular; use muted white or blue |

These sizes are practical starting points, **not measurements from the screenshots**. Preserve readable text at the terminal's viewing distance; avoid shrinking basket rows and amounts merely to fit more items. Use sentence case, short labels, and clear numeric alignment. For prices, keep currency and decimals easy to scan and align repeated values consistently.

## Layout and spacing

- Design for a landscape point-of-sale workspace when using the pictured ordering layout.
- Establish a clear separation between persistent navigation, the product-selection area, and the basket/checkout summary.
- Use an 8dp spacing rhythm. A reasonable starting layout is 24dp outer padding, 16dp between related controls, and 8dp within compact groups.
- Keep frequently used categories and order controls within comfortable reach and visible without scrolling where possible.
- Prefer a small number of clearly separated regions to dense grids of boxed content.
- The exact values should be validated against the app's actual display density and usable screen area. Do not treat the photographed screen proportions as a pixel-perfect layout specification.

## Buttons and touch controls

- **Primary action:** filled mint-green rounded rectangle with a short white label. Use it for the main progression, such as adding a selection or confirming an order.
- **Secondary action:** filled cyan or teal rounded rectangle with a white label. Use it for a useful alternative action, not another competing primary.
- **Category/filter control:** compact pill-shaped tab; show the selected category with a stronger mint or cyan fill and keep unselected options visually quieter.
- **Icon action:** use a simple, high-contrast icon for familiar utilities (for example, basket or navigation). Provide an accessible label and avoid icon-only controls for ambiguous tasks.
- Use consistent corner radii across buttons and chips; a starting range of 8–16dp is appropriate for the soft, rounded treatment shown.
- Make touch targets at least 48dp in each dimension where practical, even if the visible icon or label is smaller. Provide clear pressed, selected, disabled, and focus states.
- Keep labels brief and verb-led where possible. Avoid thin text over saturated fills.

## Product tiles, basket, and cards

- **Product tiles:** use light, softly rounded tiles against the dark canvas. Center a recognizable product image, place its name nearby, and keep price or quantity controls distinct and easy to scan.
- **Selection state:** communicate it through more than color where possible (for example, a changed outline, check mark, or quantity indicator). Avoid adding heavy shadows to every tile.
- **Basket panel:** visually separate the basket from product browsing with a slightly different blue surface or a clear boundary. Use a compact title/header with a recognizable basket action.
- **Basket rows:** present item name, quantity, and unit price in stable columns. Keep quantities and prices aligned down the list; use small rounded light fields or chips only when they represent editable values.
- **Total:** give the amount due a stronger size and weight than row prices, with enough whitespace around it. Make the primary checkout action immediately adjacent or visually connected to the total.
- Prefer flat color surfaces and spacing to nested card-on-card containers. Use shadows sparingly; the screenshots rely primarily on color separation and rounded corners.

## Navigation, iconography, and imagery

- Use a short horizontal row of category pills for primary product browsing, as shown in the catalog screen.
- Keep global utilities in a predictable edge or footer area and visually subordinate to the ordering flow.
- Use simple, consistent line icons with a common stroke weight and size. Use familiar symbols for basket, settings, navigation, and status; supplement unfamiliar icons with labels.
- Use high-quality product photography on light tiles. Crop consistently, preserve the subject, and avoid imagery competing with the total or checkout action.
- Keep brand marks separate from functional labels; a logo should not replace a screen title or an accessible text label.

## Interaction and accessibility

- Make the current step and selected product/category immediately apparent.
- Give immediate, visible feedback when a product is selected or its quantity changes, and keep the basket total synchronized.
- Prevent accidental checkout by making the primary action distinct and by showing the resulting order context before payment.
- Support Android font scaling and ensure larger text does not clip or overlap controls.
- Maintain readable contrast on navy, cyan, teal, and mint surfaces; do not encode errors or payment outcomes using color alone.
- Ensure interactive targets have accessible names, logical focus order, and visible focus/pressed feedback.
- Because this is a payment point-of-sale context, provide clear confirmation and recovery states for consequential actions; these details are not fully specified by the promo imagery.

## Suggested implementation tokens

```text
color.background       = #09265C
color.surface.deep     = #071D49
color.surface.raised   = #123D78
color.action.primary  = #57C99B
color.action.secondary= #37B9D2
color.action.tertiary = #36A9B8
color.text.primary    = #FFFFFF
color.text.secondary  = #EAF0F5
color.text.muted      = #A9BED7
spacing.unit          = 8dp
touch.target.minimum  = 48dp
```

These are **suggested starting tokens**, not official Ingenico design-system values.

## Sources

- [AXIUM CX series promotional video](https://youtu.be/mFJ_iO0bVYo?si=a3vDxZCEaSxuCoqd) — device and example application screens.
- [AXIUM CX9000 product page](https://ingenico.com/en/products-services/payment-terminals/axium-android/axium-cx9000) — product positioning and display specifications. The website presentation itself was not used as the UI reference.
- Supplied CX9000 screen images in the task — ordering, basket, product tiles, colors, and control styles.
