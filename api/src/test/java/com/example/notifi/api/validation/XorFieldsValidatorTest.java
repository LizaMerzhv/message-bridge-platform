package com.example.notifi.api.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

@XorFields(first = "a", second = "b")
class XorFieldsValidatorTestBean {
  private String a;
  private String b;

  public String getA() {
    return a;
  }

  public void setA(String a) {
    this.a = a;
  }

  public String getB() {
    return b;
  }

  public void setB(String b) {
    this.b = b;
  }
}

public class XorFieldsValidatorTest {

  private final XorFieldsValidator v = new XorFieldsValidator();

  @Test
  void true_when_exactly_one_present() {
    XorFieldsValidatorTestBean bean = new XorFieldsValidatorTestBean();
    v.initialize(XorFieldsValidatorTestBean.class.getAnnotation(XorFields.class));

    bean.setA("x");
    bean.setB(null);
    assertThat(v.isValid(bean, null)).isTrue();

    bean.setA(null);
    bean.setB("y");
    assertThat(v.isValid(bean, null)).isTrue();
  }

  @Test
  void false_when_both_or_none_or_blank() {
    XorFieldsValidatorTestBean bean = new XorFieldsValidatorTestBean();
    v.initialize(XorFieldsValidatorTestBean.class.getAnnotation(XorFields.class));

    bean.setA(null);
    bean.setB(null);
    assertThat(v.isValid(bean, null)).isFalse();

    bean.setA("x");
    bean.setB("y");
    assertThat(v.isValid(bean, null)).isFalse();

    bean.setA("");
    bean.setB("y");
    assertThat(v.isValid(bean, null)).isTrue(); // blank treated as absent

    bean.setA("x");
    bean.setB("  ");
    assertThat(v.isValid(bean, null)).isTrue(); // blank treated as absent
  }
}
