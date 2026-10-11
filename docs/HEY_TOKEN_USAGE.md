# Hey color/token usage

Authoritative palette: `approved-20261011/HEY_BRAND_PALETTE_AND_TOKENS_V1.md`. Android source: `app/src/main/res/values/hey_colors.xml`.

| Role | Resource | Use |
|---|---|---|
| Black / white brand | hey_ink / hey_paper | Supplied wordmark, adaptive icon and splash |
| Canvas / browser chrome | hey_canvas / hey_chrome | Page background / address region |
| Graphite surfaces | hey_home / hey_task / hey_primary / hey_group | Home / task / connection / grouped rows |
| Nested / icon well / divider | hey_nested / hey_icon / hey_icon_strong / hey_divider | Disclosure, inset icon wells and separators |
| Text | hey_text / hey_secondary / hey_muted | Primary, description, metadata |
| Semantic accent | hey_healthy / hey_working / hey_attention / hey_problem | Truthful status text/dot only |

Page gutters: 22dp, centered content capped around 620dp by adaptive side padding. Card radius: 26dp. Row icons: 23dp in 44–46dp wells. Minimum interactive height: 48dp. Text uses sp/intrinsic height; connection values stack at font scale >1.3. Arc cards equal size in every state, width/height grow together for system font scale. Drop is 7dp at neighboring destination, capped at 16dp (reference HTML 17/36). No active scale transform.
