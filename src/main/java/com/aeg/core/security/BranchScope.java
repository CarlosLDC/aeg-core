package com.aeg.core.security;

import java.util.Set;

/**
 * Alcance de sucursales visible para el usuario autenticado.
 * <ul>
 *   <li>{@link Visibility#ALL} — sin filtro (ADMIN)</li>
 *   <li>{@link Visibility#SCOPED} — solo {@link #branchIds()}</li>
 *   <li>{@link Visibility#NONE} — sin acceso a datos</li>
 * </ul>
 */
public record BranchScope(Visibility visibility, Set<Long> branchIds) {

	public enum Visibility {
		ALL,
		SCOPED,
		NONE
	}

	public static BranchScope all() {
		return new BranchScope(Visibility.ALL, Set.of());
	}

	public static BranchScope none() {
		return new BranchScope(Visibility.NONE, Set.of());
	}

	public static BranchScope scoped(Set<Long> branchIds) {
		if (branchIds == null || branchIds.isEmpty()) {
			return none();
		}
		return new BranchScope(Visibility.SCOPED, Set.copyOf(branchIds));
	}

	public boolean allowsBranch(Long branchId) {
		return switch (visibility) {
			case ALL -> true;
			case SCOPED -> branchIds.contains(branchId);
			case NONE -> false;
		};
	}
}
