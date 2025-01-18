package org.enricogiurin.vocabulary.api.jooq;

/*-
 * #%L
 * Vocabulary API
 * %%
 * Copyright (C) 2024 - 2025 Vocabulary Team
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.falseCondition;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.max;
import static org.jooq.impl.DSL.rowNumber;
import static org.jooq.impl.DSL.trueCondition;

import com.yourrents.services.common.searchable.EnumCombinator;
import com.yourrents.services.common.searchable.Searchable;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Select;
import org.jooq.SelectFinalStep;
import org.jooq.SelectQuery;
import org.jooq.SortField;
import org.jooq.Table;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomJooqUtils {

  public static final String EQUAL = "eq";
  public static final String NOT_EQUAL = "ne";
  public static final String GREATER_THAN = "gt";
  public static final String GREATER_THAN_OR_EQUAL = "ge";
  public static final String LESS_THAN = "lt";
  public static final String LESS_THAN_OR_EQUAL = "le";
  public static final String CONTAINS = "contains";
  public static final String CONTAINS_IGNORE_CASE = "containsIgnoreCase";
  public static final String STARTS_WITH = "startsWith";
  public static final String ENDS_WITH = "endsWith";


  public Select<?> paginate(
      DSLContext ctx,
      Select<?> original,
      int limit,
      long offset) {
    Table<?> u = original.asTable("u");
    Field<Integer> totalRows = count().over().as("total_rows");
    Field<Integer> row = rowNumber().over().as("row");

    Table<?> t = ctx
        .select(u.asterisk())
        .select(totalRows, row)
        .from(u)
        .limit(limit)
        .offset(offset)
        .asTable("t");

    Select<?> result = ctx
        .select(t.fields(original.getSelect().toArray(Field[]::new)))
        .select(
            count().over().as("actual_page_size"),
            field(max(t.field(row)).over().eq(t.field(totalRows)))
                .as("last_page"),
            t.field(totalRows),
            t.field(row),
            t.field(row).minus(inline(1)).div(limit).plus(inline(1))
                .as("current_page"))
        .from(t);
    return result;
  }

  private Condition getCondition(Searchable filter, Function<String, Field<?>> fieldMapper) {
    return getCondition(filter, fieldMapper, true);
  }

  private Condition getCondition(Searchable filter, Function<String, Field<?>> fieldMapper,
      boolean ignoreNotSupported) {
    BinaryOperator<Condition> combinator = filter.getCombinator() == EnumCombinator.AND
        ? Condition::and
        : Condition::or;
    Condition identity = filter.getCombinator() == EnumCombinator.AND
        ? trueCondition()
        : falseCondition();
    return filter.getFilter().stream()
        .filter(c -> !ignoreNotSupported
            || isFieldSupported(c.getField().toString(), fieldMapper))
        .map(c -> {
          Field<?> field = fieldMapper.apply(c.getField().toString());
          return buildStringCondition(
              field,
              c.getOperator().toString(),
              c.getValue());
        }).reduce(identity, combinator);
  }

  private SortField<?>[] getSortFields(Pageable pageable, Function<String, Field<?>> fieldMapper) {
    return getSortFields(pageable, fieldMapper, true);
  }

  private SortField<?>[] getSortFields(Pageable pageable, Function<String, Field<?>> fieldMapper,
      boolean ignoreNotSupported) {
    return pageable.getSort()
        .filter(sort -> !ignoreNotSupported
            || isFieldSupported(sort.getProperty(), fieldMapper))
        .map(sort -> {
          Field<?> field = fieldMapper.apply(sort.getProperty());
          if (sort.isAscending()) {
            return field.asc();
          } else {
            return field.desc();
          }
        }).stream().toArray(SortField[]::new);
  }

  private boolean isFieldSupported(String field, Function<String, Field<?>> fieldMapper) {
    try {
      if (fieldMapper.apply(field) != null) {
        return true;
      } else {
        return false;
      }
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public Select<?> getQueryWithConditionsAndSorts(SelectFinalStep<?> query,
      Searchable filter, Function<String, Field<?>> filterFieldMapper,
      Pageable pageable, Function<String, Field<?>> sortFieldMapper) {
    SelectQuery<?> result = query.getQuery();
    result.addConditions(getCondition(filter, filterFieldMapper));
    result.addOrderBy(getSortFields(pageable, sortFieldMapper));
    log.debug("Query with conditions and sorts: {}", result);
    return result;
  }

  /**
   * Build a condition for a field
   *
   * @param field
   * @param operator
   * @param value
   * @return
   */
  private Condition buildStringCondition(Field<?> field, String operator, Object value) {
    if (value != null) {
      Field<Object> objField = field.coerce(Object.class);
      return switch (operator) {
        case EQUAL -> field.cast(String.class).eq(value.toString());
        case NOT_EQUAL -> objField.ne(value);
        case GREATER_THAN -> objField.gt(value);
        case GREATER_THAN_OR_EQUAL -> objField.ge(value);
        case LESS_THAN -> objField.lt(value);
        case LESS_THAN_OR_EQUAL -> objField.le(value);
        case CONTAINS -> field.cast(String.class).contains(value.toString());
        case CONTAINS_IGNORE_CASE -> field.cast(String.class).containsIgnoreCase(value.toString());
        case STARTS_WITH -> field.cast(String.class).startsWith(value.toString());
        case ENDS_WITH -> field.cast(String.class).endsWith(value.toString());
        default -> throw new IllegalArgumentException("Unsupported operator: " + operator);
      };
    } else {
      return trueCondition();
    }
  }
}


