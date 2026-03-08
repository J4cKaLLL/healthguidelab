'use client';

import { useState } from 'react';

import { RecipeList } from '@/components/recipes/RecipeList';
import { useAuthUser } from '@/hooks/useAuthUser';
import { useRecipes } from '@/hooks/useRecipes';

export default function HomePage() {
  const [category, setCategory] = useState('all');
  const { hasPremiumAccess } = useAuthUser();
  const { recipes, categories, isLoading, error } = useRecipes(category);

  return (
    <main className="container">
      <header className="page-header">
        <h1>Healthy Recipes</h1>
        <p>Discover nourishing meals for every day.</p>
      </header>

      <section className="category-filter" aria-label="Recipe categories">
        {categories.map((item) => (
          <button
            key={item}
            type="button"
            className={`chip ${category === item ? 'chip-active' : ''}`}
            onClick={() => setCategory(item)}
          >
            {item}
          </button>
        ))}
      </section>

      {isLoading && <p className="empty-state">Loading recipes...</p>}
      {error && <p className="error-state">{error}</p>}
      {!isLoading && !error && <RecipeList recipes={recipes} hasPremiumAccess={hasPremiumAccess} />}
    </main>
  );
}
