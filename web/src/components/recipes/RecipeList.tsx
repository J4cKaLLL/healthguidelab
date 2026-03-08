import { Recipe } from '@/lib/types/recipe';
import { RecipeCard } from '@/components/recipes/RecipeCard';

type RecipeListProps = {
  recipes: Recipe[];
  hasPremiumAccess: boolean;
};

export function RecipeList({ recipes, hasPremiumAccess }: RecipeListProps) {
  if (!recipes.length) {
    return <p className="empty-state">No recipes found yet. Add your first recipe in Firestore.</p>;
  }

  return (
    <section className="recipe-grid" aria-label="Recipe list">
      {recipes.map((recipe) => (
        <RecipeCard key={recipe.id} recipe={recipe} hasPremiumAccess={hasPremiumAccess} />
      ))}
    </section>
  );
}
