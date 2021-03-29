package com.stackframe;

import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.property.Email;
import ezvcard.property.Organization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class VCardUtil {

    private VCardUtil() {
    }

    private static boolean isNullOrBlank(final String s) {
        return s == null || s.isBlank();
    }

    private static void cleanEmails(final VCard card) {
        final List<Email> emails = card.getEmails();
        final List<Email> toRemove = new ArrayList<>();
        for (final Email e : emails) {
            if (isNullOrBlank(e.getValue())) {
                toRemove.add(e);
            }
        }

        toRemove.forEach(x -> card.removeProperty(x));
    }

    private static void cleanOrganizations(final VCard card) {
        final List<Organization> orgs = card.getOrganizations();
        final List<Organization> toRemove = new ArrayList<>();
        for (final Organization o : orgs) {
            final List<String> values = o.getValues();
            boolean keep = false;
            for (final String s : values) {
                if (s != null && !s.isBlank()) {
                    keep = true;
                    break;
                }
            }

            if (!keep) {
                toRemove.add(o);
            }
        }

        toRemove.forEach(x -> card.removeProperty(x));
    }

    public static VCardVersion highestVersion(final Collection<VCard> cards) {
        return cards.stream().map(x -> x.getVersion()).max(Comparator.naturalOrder()).get();
    }

    public static VCard cleanup(final VCard card) {
        final VCard copy = new VCard(card);
        cleanEmails(copy);
        cleanOrganizations(copy);
        return copy;
    }

}
