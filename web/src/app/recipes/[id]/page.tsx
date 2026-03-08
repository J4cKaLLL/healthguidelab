'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';

import { PremiumLockScreen } from '@/components/recipes/PremiumLockScreen';
import { RecipeDetail } from '@/components/recipes/RecipeDetail';
import { useAuthUser } from '@/hooks/useAuthUser';
import { useRecipe } from '@/hooks/useRecipe';

export default function RecipePage() {
  const params = useParams<{ id: string }>();
  const { hasPremiumAccess, isLoading: authLoading } = useAuthUser();
  const { recipe, isLoading, error } = useRecipe(params.id);

  if (isLoading || authLoading) {
    return (
      <main className="container">
        <p className="empty-state">Loading recipe...</p>
      </main>
    );
  }

  if (error) {
    return (
      <main className="container">
        <p className="error-state">{error}</p>
        <Link href="/" className="back-home-link">Back to recipes</Link>
      </main>
    );
  }

  if (!recipe) {
    return (
      <main className="container">
        <p className="empty-state">Recipe not found.</p>
        <Link href="/" className="back-home-link">Back to recipes</Link>
      </main>
    );
  }

  if (recipe.isPremium && !hasPremiumAccess) {
    return (
      <main className="container">
        <PremiumLockScreen />
      </main>
    );
  }

  return (
    <main className="container">
      <RecipeDetail recipe={recipe} />
    </main>
  );
}
