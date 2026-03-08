# HealthGuideLab Web (Next.js + Firebase)

This folder contains the next-stage web architecture for the Healthy Recipes app.

## Firestore collection

Create a `recipes` collection with documents like:

```json
{
  "title": "Avocado Toast",
  "ingredients": ["1 avocado", "2 slices whole grain bread", "salt"],
  "preparation": "Mash the avocado and spread over toasted bread...",
  "imageUrl": "https://...",
  "isPremium": false,
  "category": "breakfast",
  "createdAt": "serverTimestamp()"
}
```

## Firebase Storage

Upload all recipe images into:

- `recipes-images/avocado-toast.jpg`

Save only the public download URL in `recipes.imageUrl`.

Helpers included:
- `uploadRecipeImage(file, fileName)` to upload into `recipes-images/`.
- `createRecipe(recipe)` to create a Firestore recipe document.

## Premium access strategy

Premium access checks both methods:

1. Firebase custom claim: `premium: true`
2. Fallback document: `users/{uid}.hasPremiumAccess === true`

## Pages and components

- `/` Home screen with recipe cards, category chips, premium badges, ingredients preview, and preparation preview
- `/recipes/[id]` Recipe detail with premium lock handling
- Reusable components:
  - `RecipeCard`
  - `RecipeList`
  - `RecipeDetail`
  - `PremiumLockScreen`

## Architecture notes

This structure is designed to scale for upcoming features:
- Favorites (add `favorites` collection + user recipe references)
- Search (query by title/category keywords)
- Subscription payments (sync subscription status into custom claims or `users` doc)
- Category expansion (keto, vegan, smoothies)

## Environment variables

Add these to `.env.local`:

```bash
NEXT_PUBLIC_FIREBASE_API_KEY=
NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=
NEXT_PUBLIC_FIREBASE_PROJECT_ID=
NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET=
NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=
NEXT_PUBLIC_FIREBASE_APP_ID=
```
