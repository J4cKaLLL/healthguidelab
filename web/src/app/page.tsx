'use client';

import { useEffect, useState } from 'react';

import { RecipeList } from '@/components/recipes/RecipeList';
import { useAuthUser } from '@/hooks/useAuthUser';
import { getRecipes } from '@/lib/firebase/recipes';
import { Recipe } from '@/lib/types/recipe';

export default function HomePage() {
  const [recipes, setRecipes] = useState<Recipe[]>([]);
  const [isLoadingRecipes, setIsLoadingRecipes] = useState(true);
  const { hasPremiumAccess } = useAuthUser();

  useEffect(() => {
    const loadRecipes = async () => {
      setIsLoadingRecipes(true);
      setRecipes(await getRecipes());
      setIsLoadingRecipes(false);
    };

    void loadRecipes();
  }, []);

  return (
    <main className="container">
      <header className="page-header">
        <h1>Healthy Recipes</h1>
        <p>Discover nourishing meals for every day.</p>
      </header>

      {isLoadingRecipes ? <p className="empty-state">Loading recipes...</p> : <RecipeList recipes={recipes} hasPremiumAccess={hasPremiumAccess} />}
    </main>
  );
}
