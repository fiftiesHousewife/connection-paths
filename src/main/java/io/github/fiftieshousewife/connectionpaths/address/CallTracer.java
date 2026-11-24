package io.github.fiftieshousewife.connectionpaths.address;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import java.util.Optional;
import java.util.Set;

// Follows an address through a call or an object creation. A call that reads an address by name gives it; a call into
// the source is followed into what it returns; otherwise, as for a builder's chain or the JDK's own helpers, what it
// is called on gives the address, or failing that its first argument. A builder's address is in its first call, so
// the later calls' arguments, such as timeouts, are passed over.
final class CallTracer {

    private static final Set<String> LOCAL_FILE_TYPES =
        Set.of("File", "FileInputStream", "FileOutputStream", "FileReader", "RandomAccessFile");
    private static final Set<String> NOT_ADDRESSES =
        Set.of("getInstance", "forName", "getLogger", "getBundle", "valueOf", "getDefault");
    private static final Set<String> JDK_HELPERS = Set.of("String", "URI", "URL", "Paths", "Path", "Objects");

    private final AddressTracer expressions;
    private final NameTracer names;

    CallTracer(final AddressTracer expressions, final NameTracer names) {
        this.expressions = expressions;
        this.names = names;
    }

    Address call(final MethodCallExpr call, final Trace trace) {
        if (NOT_ADDRESSES.contains(call.getNameAsString())) {
            return Address.builtBy(AddressText.shorten(call));
        }
        return named(call).or(() -> InetAddresses.of(call)).or(() -> returned(call, trace))
            .or(() -> fromParts(call, trace))
            .orElse(Address.builtBy(AddressText.shorten(call)));
    }

    private static Optional<Address> named(final MethodCallExpr call) {
        final String scope = scopeText(call);
        return AddressText.firstArgument(call).flatMap(Declarations::constant)
            .flatMap(text -> NamedLookups.lookedUp(scope, call.getNameAsString(), text));
    }

    private Optional<Address> returned(final MethodCallExpr call, final Trace trace) {
        final boolean jdk = JDK_HELPERS.contains(scopeText(call)) || Declarations.declaredInJdk(call);
        return jdk ? Optional.empty() : names.returnedBy(call, trace).map(a -> argumentFor(call, a, trace));
    }

    // What a method returns built from one of its parameters is built from the argument this call passes for it.
    private Address argumentFor(final MethodCallExpr call, final Address returned, final Trace trace) {
        if (!Address.PARAMETER.equals(returned.getSource())) {
            return returned;
        }
        final String parameter = returned.getValue().substring(0, returned.getValue().indexOf(" of "));
        return Declarations.of(call).filter(CallableDeclaration.class::isInstance)
            .map(d -> ((CallableDeclaration<?>) d).getParameters().stream().map(p -> p.getNameAsString()).toList())
            .filter(names -> returned.getValue().endsWith(" of " + call.getNameAsString()))
            .map(names -> names.indexOf(parameter))
            .filter(i -> i >= 0 && i < call.getArguments().size())
            .map(i -> expressions.expression(call.getArgument(i), trace)).orElse(returned);
    }

    private Optional<Address> fromParts(final MethodCallExpr call, final Trace trace) {
        return call.getScope().map(s -> expressions.expression(s, trace)).filter(AddressText::saysSomething)
            .or(() -> AddressText.firstArgument(call).map(a -> expressions.expression(a, trace))
                .filter(AddressText::isAnAddress).filter(AddressText::saysSomething));
    }

    private static String scopeText(final MethodCallExpr call) {
        return call.getScope().map(Node::toString).orElse("");
    }

    Address creation(final ObjectCreationExpr creation, final Trace trace) {
        final String type = creation.getType().getNameAsString();
        if (LOCAL_FILE_TYPES.contains(type)) {
            return Address.of(Address.LOCAL_FILE,
                AddressText.firstArgument(creation).map(AddressText::shorten).orElse(type));
        }
        return AddressText.firstArgument(creation).map(a -> expressions.expression(a, trace))
            .orElse(Address.builtBy("new " + type));
    }
}
