### HSM2NHSM (recursive)
The [HSM2NHSM.qvtr](HSM2NHSM.qvtr) transformation specifies the collapse/expansion of state diagrams

#### Overview
Every top-level State on the expanded state diagram is matched to a State in the collapsed state diagram with the same name. Transitions at the expanded sate diagram pushed up from nested States to the top-level States at the collapsed state diagram.

This version of HSM2NHSM relies on the *recursion* to retrieve Transitions from nested states. A version using *transitive closure* is also available [here](../HSM2NHSM_closure/).

#### Meta-models
| [HSM.ecore](HSM.ecore) for expanded (hierarchical) state diagrams | [NHSM.ecore](NHSM.ecore) for collapsed (non-hierarchical) state diagrams |
| --- | --- | --- |
| <img src="HSM_metamodel.png" alt="HSM metamodel" width="400px"> | <img src="NHM_metamodel.png" alt="NHSM metamodel" width="400px"> |

#### Models
| [HSM_example.xmi](HSM_example.xmi) | [NHM_example.xmi](NHM_example.xmi) |
| --- | --- | --- |
| <img src="HSM_model.png" alt="HSM model" width="250px" align="middle"/> | <img src="NHM_model.png" alt="NHSM model" width="200px" align="middle"/> |

#### History
* This example is based on the running example from the paper *JTL: a bidirectional and change propagating transformation language* by A. Cicchetti, D. Di Ruscio, R. Eramo and A. Pierantonio.
* This example is used as a running example in the submitted paper *Least-change Bidirectional Model Transformation with QVT-R and ATL* by N. Macedo and A. Cunha.