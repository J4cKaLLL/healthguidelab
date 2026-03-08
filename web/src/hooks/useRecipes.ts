'use client';

import { useEffect, useMemo, useState } from 'react';

import { getRecipes } from '@/lib/firebase/recipes';
import { Recipe } from '@/lib/types/recipe';

type UseRecipesState = {
  recipes: Recipe[];
  categories: string[];
  isLoading: boolean;
  error: string | null;
};

export function useRecipes(category: string): UseRecipesState {
  const [recipes, setRecipes] = useState<Recipe[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;

    const loadRecipes = async () => {
      setIsLoading(true);
      setError(null);

      try {
        const response = await getRecipes(category);
        if (isMounted) setRecipes(response);
      } catch {
        if (isMounted) setError('Unable to load recipes right now. Please try again.');
      } finally {
        if (isMounted) setIsLoading(false);
      }
    };

    void loadRecipes();

    return () => {
      isMounted = false;
    };
  }, [category]);

  const categories = useMemo(() => {
    const allCategories = new Set(['all', ...recipes.map((recipe) => recipe.category)]);
    return [...allCategories];
  }, [recipes]);

  return { recipes, categories, isLoading, error };
}
