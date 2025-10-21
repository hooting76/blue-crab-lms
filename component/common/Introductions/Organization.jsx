import "../../../css/Introductions/Organization.css";
import branch01 from '../../../src/assets/Branch_Univ.png';
import branch02 from '../../../src/assets/Univ_Trees.png';

function Organization() {
    return (
        <div className="blueCrabOrganization">
            <img src={branch01} alt="blueCrabOrganization"/>
            <img src={branch02} alt="blueCrabOrganization"/>
        </div>
    );
}

export default Organization;