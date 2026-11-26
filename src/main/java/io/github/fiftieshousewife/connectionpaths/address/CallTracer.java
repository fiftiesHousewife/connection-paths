package io.github.fiftieshousewife.connectionpaths.address;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// Follows an address through a call or an object creation. A call that reads an address by name gives it; a call into
// the source is followed into what it returns; otherwise, as for a builder's chain or the JDK's own helpers, what it
// is called on gives the address, or failing that its first argument. What a method returns from a stream it is given
// is what it reads. A builder's address is in its first call, so
// the later calls' arguments, such as timeouts, are passed over.
final class CallTracer {

    private static final Set<String> LOCAL_FILE_TYPES =
        Set.of("File", "FileInputStream", "FileOutputStream", "FileReader", "RandomAccessFile");
    private static final Set<String> NOT_ADDRESSES =
        Set.of("getInstance", "forName", "getLogger", "getBundle", "valueOf", "getDefault");
    private static final Set<String> JDK_SETTING_READERS = Set.of("getProperty", "getString", "getObject", "get");
    private static final Set<String> READ_TYPES = Set.of("File", "Path", "Scanner", "Readable");
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
            .flatMap(text -> NamedLookups.lookedUp(scope, call.getNameAsString(), text))
            .filter(address -> !Address.SETTING.equals(address.getSource()) || readsSettings(call));
    }

    // A dotted name passed to the JDK is an address, as InetAddress.getByName("db.acme.de") is, but for the JDK's own
    // readers of settings, such as Properties.getProperty.
    private static boolean readsSettings(final MethodCallExpr call) {
        return JDK_SETTING_READERS.contains(call.getNameAsString()) || !Declarations.declaredInJdk(call);
    }

    private Optional<Address> returned(final MethodCallExpr call, final Trace trace) {
        final boolean jdk = JDK_HELPERS.contains(scopeText(call)) || Declarations.declaredInJdk(call);
        return jdk ? Optional.empty() : names.returnedBy(call, trace).map(a -> argumentFor(call, a, trace));
    }

    // What a method returns built from one of its parameters is built from the argument this call passes for it; but
    // what it returns from a stream, a reader, a file or a path it is given is what it reads, known only at run time.
    private Address argumentFor(final MethodCallExpr call, final Address returned, final Trace trace) {
        if (!Address.PARAMETER.equals(returned.getSource())) {
            return returned;
        }
        final String parameter = returned.getValue().substring(0, returned.getValue().indexOf(" of "));
        final List<Parameter> parameters = Declarations.of(call).filter(CallableDeclaration.class::isInstance)
            .filter(d -> returned.getValue().endsWith(" of " + call.getNameAsString()))
            .map(d -> List.copyOf(((CallableDeclaration<?>) d).getParameters())).orElse(List.of());
        final int at = parameters.stream().map(Parameter::getNameAsString).toList().indexOf(parameter);
        if (at < 0 || at >= call.getArguments().size()) {
            return returned;
        }
        return isRead(parameters.get(at)) ? Address.builtBy("what " + AddressText.shorten(call) + " reads")
            : expressions.expression(call.getArgument(at), trace);
    }

    private static boolean isRead(final Parameter parameter) {
        final String type = parameter.getType().asString();
        return type.endsWith("Reader") || type.endsWith("InputStream") || READ_TYPES.contains(type);
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
        final List<Address> arguments = creation.getArguments().stream().map(a -> expressions.expression(a, trace))
            .toList();
        if (arguments.isEmpty()) {
            return Address.builtBy("new " + type);
        }
        return isANamePart(arguments) ? fromTheRest(arguments, type) : arguments.get(0);
    }

    // A first argument that is a literal but no address, such as a protocol's name, with more after it: new URL("jar",
    // "", -1, entry) is made from its parts. A command, such as new ProcessBuilder("ls"), is its only argument.
    private static boolean isANamePart(final List<Address> arguments) {
        return arguments.size() > 1 && arguments.get(0).isLiteral() && !AddressText.isAnAddress(arguments.get(0));
    }

    private static Address fromTheRest(final List<Address> arguments, final String type) {
        return arguments.stream().skip(1).filter(a -> AddressText.isAnAddress(a) && AddressText.saysSomething(a))
            .findFirst().orElse(Address.builtBy("new " + type));
    }
}
