from . import logic


def AND(arg, *args):
    v = logic.as_sql_block(arg)
    for a in args:
        v &= a
    return v


def OR(arg, *args):
    v = logic.as_sql_block(arg)
    for a in args:
        v |= a
    return v


def comma_sep(arg, *args):
    v = logic.as_sql_block(arg)
    for a in args:
        v = v.comma(a)
    return v


def count(expr, as_=None):
    f = logic.Func('COUNT', logic.as_var(expr))
    if as_:
        f = f.as_(as_)
    return f


def not_(arg):
    return logic.Func('not', arg)


def any(arg):
    return logic.Func('any', arg)


def now():
    return logic.SqlBlock(logic.RawDangerous('now()'))


def cast(v, cast_type):
    return logic.as_sql_block(v).cast(cast_type)


def upper(string):
    return logic.Func('upper', string)


def lower(string):
    return logic.Func('lower', string)


def length(string):
    return logic.Func('length', string)


def left(string, n):
    return logic.Func('left', string, n)


def right(string, n):
    return logic.Func('right', string, n)


def extract(expr):
    return logic.Func('extract', expr)


def sqrt(n):
    return logic.as_sql_block(n).sqrt()


def abs(n):
    return logic.as_sql_block(n).abs()


def factorial(n):
    return logic.as_sql_block(n).factorial()


def position(substring, string):
    return logic.Func('position', logic.SqlBlock(substring).in_(string))


def substring(string, pattern, for_=None):
    a = logic.SqlBlock(string).from_(pattern)
    if for_:
        a = a.for_(for_)
    return logic.Func('substring', a)


def to_tsvector(arg1, document=None):
    return logic.Func('to_tsvector', arg1) if document is None else logic.Func('to_tsvector', arg1, document)


def to_tsquery(arg1, text=None):
    return logic.Func('to_tsquery', arg1) if text is None else logic.Func('to_tsquery', arg1, text)
