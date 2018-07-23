package info.yangguo.waf.validator;

import javax.validation.GroupSequence;
import javax.validation.groups.Default;

@GroupSequence({Default.class, NotExist.class})
public interface NotExistSequence {
}
