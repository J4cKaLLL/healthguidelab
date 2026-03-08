'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useEffect, useState } from 'react';

import { PremiumLockScreen } from '@/components/recipes/PremiumLockScreen';
import { RecipeDetail } from '@/components/recipes/RecipeDetail';
import { useAuthUser } from '@/hooks/useAuthUser';
import { getRecipeById } from '@/lib/firebase/recipes';
import { Recipe } from '@/lib/types/recipe';

export default function RecipePage() {
  const params = useParams<{ id: string }>();
  const [recipe, setRecipe] = useState<Recipe | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const { hasPremiumAccess, isLoading: authLoading } = useAuthUser();

  useEffect(() => {
    const loadRecipe = async () => {
      setIsLoading(true);
      setRecipe(await getRecipeById(params.id));
      setIsLoading(false);
    };

    if (params.id) void loadRecipe();
  }, [params.id]);

  if (isLoading || authLoading) {
    return <main className="container"><p className="empty-state">Loading recipe...</p></main>;
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
    return <main className="container"><PremiumLockScreen /></main>;
  }

  return <main className="container"><RecipeDetail recipe={recipe} /></main>;
}
