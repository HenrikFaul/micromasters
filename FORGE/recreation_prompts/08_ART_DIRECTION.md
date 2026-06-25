# 08 — Art Direction (Layer 24)

## Aesthetic

**"Quiet enterprise workbench"** — Linear / Vercel dashboard / Stripe console rokon. Hűvös semleges szürkék, egyetlen indigo akcent, magas információsűrűség, tiszta tipográfia, finom mikro-animációk. Soha nem dekoratív, mindig funkcionális.

## Tokens (`src/styles.css`)

Tailwind v4 `@theme` szintaxis, **oklch** színek (nem hex). Light + dark mód.

```css
@import "tailwindcss";
@import "tw-animate-css";

@theme {
  --font-sans: "Inter Variable", ui-sans-serif, system-ui, sans-serif;
  --font-mono: "JetBrains Mono", ui-monospace, "Menlo", monospace;
  --radius: 0.5rem;
}

:root {
  --background:           oklch(0.99 0 0);
  --foreground:           oklch(0.18 0.02 264);
  --muted:                oklch(0.96 0.005 264);
  --muted-foreground:     oklch(0.50 0.02 264);
  --card:                 oklch(1 0 0);
  --card-foreground:      var(--foreground);
  --popover:              oklch(1 0 0);
  --popover-foreground:   var(--foreground);
  --primary:              oklch(0.52 0.18 265);  /* indigo */
  --primary-foreground:   oklch(0.99 0 0);
  --secondary:            oklch(0.95 0.01 264);
  --secondary-foreground: var(--foreground);
  --accent:               oklch(0.95 0.02 265);
  --accent-foreground:    var(--primary);
  --destructive:          oklch(0.55 0.22 27);
  --destructive-foreground: oklch(0.99 0 0);
  --success:              oklch(0.62 0.16 152);
  --warning:              oklch(0.72 0.16 75);
  --border:               oklch(0.92 0.005 264);
  --input:                oklch(0.92 0.005 264);
  --ring:                 oklch(0.52 0.18 265);

  --sidebar:                       oklch(0.985 0 0);
  --sidebar-foreground:            var(--foreground);
  --sidebar-border:                oklch(0.93 0.005 264);
  --sidebar-accent:                oklch(0.95 0.02 265);
  --sidebar-accent-foreground:     var(--primary);

  /* gradients & shadows */
  --gradient-primary: linear-gradient(135deg, oklch(0.55 0.18 265), oklch(0.65 0.15 285));
  --shadow-elegant:   0 10px 30px -10px color-mix(in oklab, var(--primary) 30%, transparent);
  --shadow-card:      0 1px 2px 0 oklch(0 0 0 / 0.04), 0 1px 3px 0 oklch(0 0 0 / 0.06);

  /* motion */
  --ease-emphasized: cubic-bezier(0.2, 0, 0, 1);
  --duration-fast:   140ms;
  --duration-base:   220ms;
}

.dark {
  --background:           oklch(0.16 0.01 264);
  --foreground:           oklch(0.96 0 0);
  --muted:                oklch(0.22 0.01 264);
  --muted-foreground:     oklch(0.66 0.02 264);
  --card:                 oklch(0.19 0.01 264);
  --card-foreground:      var(--foreground);
  --popover:              oklch(0.19 0.01 264);
  --popover-foreground:   var(--foreground);
  --primary:              oklch(0.70 0.16 265);
  --primary-foreground:   oklch(0.14 0.01 264);
  --secondary:            oklch(0.24 0.01 264);
  --secondary-foreground: var(--foreground);
  --accent:               oklch(0.26 0.02 265);
  --accent-foreground:    oklch(0.80 0.12 265);
  --destructive:          oklch(0.62 0.20 27);
  --success:              oklch(0.68 0.16 152);
  --warning:              oklch(0.78 0.15 75);
  --border:               oklch(0.27 0.01 264);
  --input:                oklch(0.27 0.01 264);
  --ring:                 oklch(0.70 0.16 265);
  --sidebar:              oklch(0.17 0.01 264);
  --sidebar-foreground:   var(--foreground);
  --sidebar-border:       oklch(0.24 0.01 264);
  --sidebar-accent:       oklch(0.26 0.02 265);
  --sidebar-accent-foreground: oklch(0.80 0.12 265);
}

@theme inline {
  --color-background: var(--background);
  --color-foreground: var(--foreground);
  --color-card: var(--card);
  --color-card-foreground: var(--card-foreground);
  --color-popover: var(--popover);
  --color-popover-foreground: var(--popover-foreground);
  --color-primary: var(--primary);
  --color-primary-foreground: var(--primary-foreground);
  --color-secondary: var(--secondary);
  --color-secondary-foreground: var(--secondary-foreground);
  --color-muted: var(--muted);
  --color-muted-foreground: var(--muted-foreground);
  --color-accent: var(--accent);
  --color-accent-foreground: var(--accent-foreground);
  --color-destructive: var(--destructive);
  --color-destructive-foreground: var(--destructive-foreground);
  --color-success: var(--success);
  --color-warning: var(--warning);
  --color-border: var(--border);
  --color-input: var(--input);
  --color-ring: var(--ring);
  --color-sidebar: var(--sidebar);
  --color-sidebar-foreground: var(--sidebar-foreground);
  --color-sidebar-border: var(--sidebar-border);
  --color-sidebar-accent: var(--sidebar-accent);
  --color-sidebar-accent-foreground: var(--sidebar-accent-foreground);
  --radius-lg: var(--radius);
  --radius-md: calc(var(--radius) - 2px);
  --radius-sm: calc(var(--radius) - 4px);
}
```

## Typography
- Display / titles: Inter Variable, 600/700, tracking-tight
- Body: Inter, 400/500
- Code / paths / hashes: JetBrains Mono, 11–13px
- Page H1: text-[20px] font-semibold
- Card title: text-[13.5px] font-medium
- Table cell: text-[12.5px]
- Sidebar item: text-[13px]
- Uppercase section labels: text-[10.5px] tracking-[0.12em] uppercase muted

## Motion
- `--duration-fast` (140ms) for hover/active state
- `--duration-base` (220ms) for drawers/dialogs/page transitions
- Easing `--ease-emphasized`
- No spring overshoot; this is a workbench, not a marketing site.

## Iconography
- `lucide-react` everywhere, stroke 1.8, size 3.5/4 in nav, 4 in buttons
- Status icons: `CheckCircle2` (success), `AlertTriangle` (warning), `XCircle` (error), `Info` (info)

## Badges (status colors)
| Status | bg / text |
|---|---|
| `draft` | muted / muted-foreground |
| `review` | warning/15 / warning |
| `active` | success/15 / success |
| `deprecated` | destructive/15 / destructive |
| `passed` | success/15 / success |
| `failed` | destructive/15 / destructive |
| `pending` | accent / accent-foreground |
| role `owner` | primary/15 / primary |
| role `editor` | accent / accent-foreground |
| role `operator` | warning/15 / warning |
| role `viewer` | muted / muted-foreground |

## Anti-patterns (no-go)
- Purple gradients on white (generic AI look)
- Glassmorphism / heavy blur
- Big rounded "playful" buttons
- Bright saturated colors as page background
- Decorative illustrations
