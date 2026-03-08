'use client';

import { useEffect, useState } from 'react';

import { getRecipeById } from '@/lib/firebase/recipes';
import { Recipe } from '@/lib/types/recipe';

type UseRecipeState = {
  recipe: Recipe | null;
  isLoading: boolean;
  error: string | null;
};

export function useRecipe(id: string | undefined): UseRecipeState {
  const [recipe, setRecipe] = useState<Recipe | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) {
      setIsLoading(false);
      return;
    }

    let isMounted = true;

    const loadRecipe = async () => {
      setIsLoading(true);
      setError(null);

      try {
        const response = await getRecipeById(id);
        if (isMounted) setRecipe(response);
      } catch {
        if (isMounted) setError('Unable to load this recipe right now.');
      } finally {
        if (isMounted) setIsLoading(false);
      }
    };

    void loadRecipe();

    return () => {
      isMounted = false;
    };
  }, [id]);

  return { recipe, isLoading, error };
}
